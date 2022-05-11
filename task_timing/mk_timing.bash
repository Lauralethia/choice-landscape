#!/usr/bin/env bash
#
# generate timings for MR task with decaying ITI and catch trials
# TODO: consider variable ISI "zzz"
#
# 20220510WF - init
#
[ -v DRYRUN ] && DRYRUN=echo || DRYRUN=
warn(){ echo "$@" >&2; }

# hard coded timings from piloting
# NB: RT probably different in MR comared to amazon turk

MEAN_RT=0.580     # but can be up to 1.5
ZZZ_TIME=1        # hardcode as one second always? could use decay?
WALK_TIME=0.534   # always take the same time to walk to feedback
FBK_TIME=1        # always 1 second of feedback

# average trial time is 2.1 seconds
# mean iti should be ~3 (exp disp)
# want maybe 100 trials at 5s per trial+iti = 500s total
# 500 + 100s if never responded

TR=1.3

trial_counts(){
  # 2/3 of trials have the good choice, 1/3 without the good choice
  # 1/3 of total as catch ("zzz")
  local total="$1"
          good=$(printf "%.0f" $(bc -l <<< ".666*.666*$total"))
    good_catch=$(printf "%.0f" $(bc -l <<< ".333*.666*$total"))
        nogood=$(printf "%.0f" $(bc -l <<< ".666*.333*$total"))
  nogood_catch=$(printf "%.0f" $(bc -l <<< ".333*.333*$total"))
  total_nocatch=$(( $good + $nogood ))
  total_catch=$(( $good_catch + $nogood_catch))
  # quick look. might have added or lost a trial
  new_total=$(( $good + $good_catch + $nogood + $nogood_catch ))
  [[ $total -ne $new_total ]] && warn "WARNING: wanted $total trials, but using $new_total" 
  echo $good $good_catch $nogood $nogood_catch $total_nocatch $total_catch
}

# lisp macro a la tcl: via unsafe string interpolation
# set variables usinng trial_counts where it's eval'ed
# here so we dont have to keep changing the read. just update (likely append) variable names
MACRO_TRIAL_COUNTS='read good good_catch nogood nogood_catch total_nocatch total_catch <<< "$(trial_counts $total)"'



# from https://github.com/LabNeuroCogDevel/slipstask/tree/master/timing
parse_decon(){
   # widen deconvolve output of norm std dev tests
   # so we can collect everything later in one file
   # see test for output/input
   perl -slne '
        $key=$2 if /(Gen|Stim).*: ([^ ]*)/;
        $h{$name}{"${key}_$1"}=$2 if /^\W+(LC|h).*=.*?([0-9.]+)/;
        END{
          @vals=sort (keys %{$h{(keys %h)[0]}});
          print join("\t","name",@vals);
          for my $f (keys %h){
            %_h = %{$h{$f}};
            print join("\t",$f, @_h{@vals} )
          }
    }' -- -name="$1"
}

mktiming(){
  total="$1"; shift         # expecting 
  total_runtime="$1"; shift # something like 540
  eval $MACRO_TRIAL_COUNTS # set good good_catch ...
  
  # second arg should be seed. can use random if not provided
  # TODO: max $RANDOM vs max make_random_timing seed
  [ $# -ne 0 ] && seed="$1" || seed="$RANDOM" 

  [ -d $seed ] && warn "# have $seed dir, skipping" && return 0
  mkdir -p $seed
  (cd $seed
  make_random_timing.py \
     -tr $TR \
     -num_runs 1 -run_time $total_runtime        \
     -pre_stim_rest 6 -post_stim_rest 6           \
     -rand_post_stim_rest no                      \
     -add_timing_class choice_w_good  "$MEAN_RT"  \
     -add_timing_class choice_wo_good "$MEAN_RT"  \
     -add_timing_class walk           "$WALK_TIME"\
     -add_timing_class feedback       "$FBK_TIME" \
     -add_timing_class zzz            $ZZZ_TIME \
     \
     -add_timing_class nobreak 0 0 0 dist=INSTANT \
     -add_timing_class iti 0 -1 8     \
     \
     -add_stim_class good_c          "$good"          choice_w_good  nobreak \
     -add_stim_class nogood_c        "$nogood"        choice_wo_good nobreak \
     -add_stim_class g_fbk_c         "$good"          feedback nobreak       \
     -add_stim_class g_walk_c        "$good"          walk nobreak           \
     -add_stim_class ng_fbk_c        "$nogood"        feedback iti \
     -add_stim_class ng_walk_c       "$nogood"        walk nobreak           \
     -add_stim_class g_catch_c       "$good_catch"    choice_w_good  nobreak     \
     -add_stim_class ng_catch_c      "$nogood_catch"  choice_wo_good nobreak     \
     -add_stim_class g_zzz_c         "$good_catch"    zzz iti     \
     -add_stim_class ng_zzz_c        "$nogood_catch"  zzz iti     \
     `:          g ng gfb gwk ngf ngw gc ngc gz ngz -- not sure this works with ordered_stim`\
     -max_consec 3  3   0   0   0   0  2   2  0   0 \
     -ordered_stimuli good_c g_walk_c g_fbk_c \
     -ordered_stimuli nogood_c ng_walk_c ng_fbk_c \
     -ordered_stimuli g_catch_c  g_zzz_c  \
     -ordered_stimuli ng_catch_c ng_zzz_c \
     -show_timing_stats                 \
     -make_3dd_contrasts                \
     -write_event_list events.txt \
     -save_3dd_cmd decon.tcsh      \
     -seed $seed                        \
     -prefix stimes > mktiming.log

  tcsh decon.tcsh > decon.log
  parse_decon $seed < decon.log > stddevtests.tsv
  1d_tool.py -cormat_cutoff 0.1 -show_cormat_warnings -infile X.stimes.xmat.1D > timing_cor.txt
)
}

# if not sourced (as in testing), run as command
if ! [[ "$(caller)" != "0 "* ]]; then
  set -euo pipefail
  trap 'e=$?; [ $e -ne 0 ] && echo "$0 exited in error $e"' EXIT
  [ $# -eq 0 ] && warn "USAGE: $0 ntrails total_dur # e.g. 100 500 " && exit 1

  # potentially man output directories. collect in one place
  [ ! -d out ] && mkdir out
  cd out

  mktiming "$@"
  exit $?
fi

####
# testing with bats. use like
#   bats ./mk_timing.bash --verbose-run
####
if  [[ "$(caller)" =~ /bats.*/preprocessing.bash ]]; then
function trialcount_test { #@test 
  local total=12
  eval $MACRO_TRIAL_COUNTS
  warn "'$good' '$nogood' '$good_catch' '$nogood_catch'"
  [ $good -eq 5 ]
  [ $nogood -eq 3 ]
  [ $good_catch -eq 3 ]
  [ $nogood_catch -eq 1 ]
}
function parse_test { #@test 

 output="$(cat <<HERE| parse_decon XXX
Stimulus: ng_zzz_c
  h[ 0] norm. std. dev. =   3.3897

General Linear Test: good_c-nogood_c
  LC[0] norm. std. dev. =   5.6284
HERE
)"
 
 warn "$output"
 [[ $output =~ ng_zz_c_h ]]
 [[ $output =~ good_c-nogood_c_LC ]]
 [[ $output =~ 3.3897 ]]
}
fi
