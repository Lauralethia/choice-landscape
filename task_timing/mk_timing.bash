#!/usr/bin/env bash
#
# generate timings for MR task with decaying ITI and catch trials
# TODO:
#  * consider variable ISI "ZZZ_TIME" (see comment in var def)
#  * sed in more GLTs to deconvolve
#
# 20220510WF - init
#
[ -v DRYRUN ] && DRYRUN=echo || DRYRUN=
warn(){ echo "$@" >&2; }

# hard coded timings from piloting
# NB: RT probably different in MR comared to amazon turk

TR=1.3
MEAN_RT=0.580     # but can be up to 1.5
WALK_TIME=0.534   # always take the same time to walk to feedback
FBK_TIME=1        # always 1 second of feedback
ZZZ_TIME=1        # hardcode as one second always
#ZZZ_TIME="0.5 1 3 dist=decay" between .5 and 3 with mean of 1. exp distributed

# save directory prefix incase we update e.g. GLT SYMS or timings
# but want to hold onto old
# will already be organized by total runtime and include total_trials
#   outdir=${total_runtime}s/${PREFIX}_${total}_$seed
PREFIX=v1

# average trial time is 2.1 seconds
# mean iti should be ~3 (exp disp)
# want maybe 100 trials at 5s per trial+iti = 500s total
# 500 + 100s if never responded



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
# lisp macro a la tcl: unsafe string interpolation
# set variables usinng trial_counts where it's eval'ed
# here so we dont have to keep changing the read. just update (likely append) variable names
# should match output of trail_counts function
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

add_glt(){
   # add glts to decon command file
   # needs already have e.g. '-numglt 3' and '-x1D'
   # input is file and then any number of contrast+label pairs like
   #   add_glt decon.tsch 'a -b' 'a-b' 'c +.5*d' 'c_halfd'
   cmd_file="$1";shift
   nglt=$(grep -Po '(?<=-num_glt )\d+' $cmd_file||echo "")
   [ -z "$nglt" ] && warn "ERROR: no -num_glt in '$cmd_file'" && exit 1
   newn=$nglt
   glts=""
   while [ $# -gt 0 ]; do
      let newn++
      glts="$glts -gltsym 'SYM: $1' -glt_label $newn $2"
      shift 2
   done

   sed -e "s/-num_glt $nglt/-num_glt $newn/" -i $cmd_file
   sed -e "s;-x1D;$glts -x1D;" -i $cmd_file
}

mktiming(){
  total="$1"; shift         # expecting
  total_runtime="$1"; shift # something like 540
  eval $MACRO_TRIAL_COUNTS # set good good_catch ...

  # second arg should be seed. can use random if not provided
  # TODO: max $RANDOM vs max make_random_timing seed
  [ $# -ne 0 ] && seed="$1" || seed="$RANDOM"

  outdir=${total_runtime}s/${PREFIX}_${total}_$seed
  [ -d $outdir ] && warn "# have $outdir dir, skipping" && return 0
  mkdir -p $outdir
  (cd $outdir
  make_random_timing.py \
     -tr $TR \
     -num_runs 1 -run_time $total_runtime        \
     -pre_stim_rest 6 -post_stim_rest 6           \
     -rand_post_stim_rest no                      \
     -add_timing_class s_choice_w_good  "$MEAN_RT"  \
     -add_timing_class s_choice_wo_good "$MEAN_RT"  \
     -add_timing_class s_walk           "$WALK_TIME"\
     -add_timing_class s_feedback       "$FBK_TIME" \
     -add_timing_class s_zzz            $ZZZ_TIME \
     \
     -add_timing_class nobreak 0 0 0 dist=INSTANT \
     -add_timing_class iti 0 -1 8     \
     \
     -add_stim_class good          "$good"          s_choice_w_good  nobreak \
     -add_stim_class g_fbk         "$good"          s_feedback nobreak       \
     -add_stim_class g_walk        "$good"          s_walk nobreak           \
     -add_stim_class g_catch       "$good_catch"    s_choice_w_good  nobreak     \
     -add_stim_class g_zzz         "$good_catch"    s_zzz iti     \
     \
     -add_stim_class nogood        "$nogood"        s_choice_wo_good nobreak \
     -add_stim_class ng_fbk        "$nogood"        s_feedback iti \
     -add_stim_class ng_walk       "$nogood"        s_walk nobreak           \
     -add_stim_class ng_catch      "$nogood_catch"  s_choice_wo_good nobreak     \
     -add_stim_class ng_zzz        "$nogood_catch"  s_zzz iti     \
     `:          g ng gfb gwk ngf ngw gc ngc gz ngz -- not sure this works with ordered_stim`\
     -max_consec 3  3   0   0   0   0  2   2  0   0 \
     -ordered_stimuli good g_walk g_fbk \
     -ordered_stimuli nogood ng_walk ng_fbk \
     -ordered_stimuli g_catch  g_zzz  \
     -ordered_stimuli ng_catch ng_zzz \
     -show_timing_stats                 \
     -make_3dd_contrasts                \
     -write_event_list events.txt \
     -save_3dd_cmd decon.tcsh      \
     -seed $seed                        \
     -prefix stimes > mktiming.log

  #TODO: better contrasts?

  add_glt decon.tcsh \
     'good +nogood -g_fbk -ng_fbk' 'choice-fbk' \
     '.6*good -.3*g_catch'    'good_and_catch' \
     '.6*nogood -.3*ng_catch' 'nogood_and_catch' \
     'nogood +nogood +g_catch +ng_catch' 'choice'

  tcsh decon.tcsh > decon.log
  parse_decon "${PREFIX}-${total_runtime}-${total}-${seed}" < decon.log > stddevtests.tsv
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
 [[ $output =~ ng_zzz_c_h ]]
 [[ $output =~ good_c-nogood_c_LC ]]
 [[ $output =~ 3.3897 ]]
}
function add_glt_test { #@test
   f=/tmp/glttestfile
   echo "-num_glt 4 -x1D " > $f
   run add_glt $f 'a +b' 'a_P_b' 'a -b' 'a-b'
   warn "file: '$(cat $f)'"
   grep "\-num_glt 6" $f
   grep "SYM: a +b" $f
   grep "\-glt_label 6 a-b" $f
}
fi
