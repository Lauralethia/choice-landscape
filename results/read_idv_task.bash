#!/usr/bin/env bash
#
# convert json to timing dataframe
#  likely run by make. output to bea_res
# 20220825WF - init
# 20221231WF - generate info if in outputname, collapse all if input is 'all'

DBJSON_CMD="../../task/results/dbjsontotsv.jq"
DBJSON_SMRY_CMD="../../task/results/extra_info.jq"

get_where(){
   local idv_json where=unknown
   idv_json="$1"; shift
   case "$idv_json" in
     */MR/*) where=mr;;
     */EEG/*) where=eeg;;
     */practice/*) where=practice;;
   esac
   echo $where
}
# see zero ages:
#  for f in /Volumes/L/bea_res/Data/Tasks/Habit/*/1*_2*/*.tsv ; do mlr --tsv cut -f id,age then head -n 1 $f|sed "1d;s:\$:\t$(basename $(dirname $f)):"; done
# ages from gcal:
#  perl -F'\t' -slane '$F[3] =~s/-//g; print "_$F[3]) age=$2 # $1" if /Habit.*x1.*(Scan|EEG).*?([0-9.]+)yo/'  ~/src/db/pull_from_sheets/txt/all_gcal_events.tsv
get_age(){
   local age=0
   local ld8="$1"
   case "$ld8" in
      *) age=0;; # 24?
   esac
   if [[ "$age" == 0 ]]; then
      age=$(ld8info "$ld8" | awk '(NR==2){print $2}')
      ! [[ "$age" =~ ^[0-9.]+ ]] && warn "# $ld8: no age in db" && age=0
   fi
   echo $age
}
add_info_from_json(){
   idv_json="$1"; shift
   read id vdate <<< $(get_id_date "$idv_json")
   [ -z "$vdate" ] && return 1

   local age where
   where=$(get_where "$idv_json")
   age=$(get_age "${id}_$vdate")
   add_info "$id" "$vdate" "$where" "$age"
}
add_info(){
   local id vdate where age
   id="$1"; shift
   vdate="$1"; shift
   where="$1"; shift
   age="$1"; shift
   # TODO: do we need other suvery options? like:  \"understand\":null,
   sed "1s/^/[{\"id\": \"$id\", \"timepoint\": \"$vdate\", \"task\": \"habit_$where\", \"run_number\": \"1\", \"ver\":\"${where}_1\", \"json\":/;\$s/\$/}]/; s/survey\":\[\]/survey\":{\"age\": \"$age\"}/;" 
}

extract_json(){
  # 20221020
  # EEG/11877_20220909/11877_loeffeeg_habit_20220805_1_nover.json has just json field
  local idv_json="$1"; shift
  cut -c1-10 "$idv_json" |
   grep -q '^{"json":"{' >&2  &&
   jq -r .json "$idv_json" || 
   cat "$idv_json"
}

# unix epoch from browser in json to yyyymmdd
date_from_json(){
   date -d @"$(extract_json "$1" | jq '."start-time".browser/1000')"  +%Y%m%d || echo ""
}

get_id_date(){
   # mr task javascript used date wrong! month is wrong and uses day of week instead day
   # unix time stamp is still good though
   ! [[ $1 =~ ([0-9]{5})_([0-9]{8}) ]] && warn "no id in '$1'" && return 1
   local id=${BASH_REMATCH[1]}
   local vdate=${BASH_REMATCH[2]}
   vdate_file="$(date_from_json "$1")"
   # use vdate if vdate_file fails
   echo "$id ${vdate_file:-$vdate}"
}
write_task_tsv(){
   idv_json="$1"; shift
   extract_json "$idv_json"|
    add_info_from_json "$idv_json" |
    $DBJSON_CMD |
    ./read_idv.R
}

write_task_info(){
   idv_json="$1"; shift
   extract_json "$idv_json"|
    add_info_from_json "$idv_json" |
    $DBJSON_SMRY_CMD
}
_read_idv_task() {
   [ $# -ne 1 -a $# -ne 2 ] && warn "have $# args. want 1 or 2
   USAGE: $0 task.json|all [output.info.tsv]" && exit 1
   idv_json="$1"; shift

   # probably should be it's own script. but most of the good stuff is in this file anyway
   if [[ $idv_json == "all" ]]; then
      warn "# all json files to single large json with age. to stdout"
      concat_jsons
      exit $?
   fi

   read -r id vdate <<< $(get_id_date "$idv_json")
   [ -z "$vdate" ] && warn "no vdate in '$idv_json'" && return 1

   [ $# -eq 0 ] && out=info/${id}_${vdate}_habit.tsv || out="$1"
   ! test -d "$(dirname "$out")" && mkdir "$_"

   if [[ $out =~ info.tsv$ ]]; then
      warn "# saving info (instead of row per trail tsv) to '$out'"
      write_task_info "$idv_json" > "$out"
   else
      warn "# saving line/trial to '$out'"
      write_task_tsv "$idv_json" > "$out"
   fi

  return 0
}

# combine all
concat_jsons() {
  local i=0
  local allfiles=(/Volumes/L/bea_res/Data/Tasks/Habit/*/1*_2*/*_run-*.json)
  echo '['
  for idv_json in "${allfiles[@]}"; do
   extract_json "$idv_json"| # remove {'json': ...} if it exists
      add_info_from_json "$idv_json" | # wrap add_info (age,where,timepoint)
      sed 's/^\[//; s/\],\?$//' # unnest indvidual from array

   # comma separate visits. but no comma on last
   
   [ $((++i)) -lt ${#allfiles[@]} ] && echo , || echo
  done
  echo ']'
}

eval "$(iffmain _read_idv_task)"

####
# testing with bats. use like
#   bats ./read_idv_task.bash --verbose-run
####
function age_test { #@test 
   run get_age 11883_2022
   [[ "$output" == 21 ]]
}
function where_test { #@test 
   run get_where /Volumes/L/bea_res/Data/Tasks/Habit/MR/11878_20220823/sub-11878_task-mr_habit_ses-20220702_run-1_1661280864186.json
   [[ "$output" == mr ]]
   run get_where junk
   [[ "$output" == unknown ]]
}
function get_id_date_test { #@test
   # mr writes out wrong yyymmdd in filename!
   run get_id_date /Volumes/L/bea_res/Data/Tasks/Habit/MR/11878_20220823/sub-11878_task-mr_habit_ses-20220702_run-1_1661280864186.json
   [[ "$output" == "11878 20220823" ]]

   output=$(get_id_date 11878_20220702.json)
   [[ "$output" == "11878 20220702" ]]
}

function json_test { #@test
   id=1 vdate=2 where=mr age=99
  run add_info "$id" "$vdate" "$where" "$age" <<< '{"survey":[]}'
  [[ "$output" =~ 99 ]]

  output=$(add_info "$id" "$vdate" "$where" "$age" <<< '{"survey":[], "events":[{}]}'|$DBJSON_CMD)
  echo "o:$output" >&2
  [[ $output =~ 99 ]]
}

function add_info_from_json_test { #@test
   run add_info_from_json /Volumes/L/bea_res/Data/Tasks/Habit/MR/11878_2022*/sub-11878_*_run-1_*.json <<< '{"survey":[]}'
  [[ "$output" =~ habit_mr ]]
  [[ "$output" =~ 26 ]]
}
