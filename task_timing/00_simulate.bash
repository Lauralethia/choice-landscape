#!/usr/bin/env bash
NSIMS=1000
NTRIALS=45  # x3 =>  trials
MINITI=1.5  # 1
# running 1 and 1.5 to check timing is not a constraint
# TOTAL_DUR=185 # x3 => 9.25 minutes

# 20220728
# NTRIALS=34  # x3 => 102 trials
# TOTAL_DUR=134 # x3 (+3 first) (+ 5 -1.5 last iti)=>
# VINFO="-nocatch-qwalk"

NTRIALS=50       # x3 => 150 trials
TOTAL_DUR=200.34 # 
#  (200.2 + 3.233 - 1.5)*3/1.3 == 466 (605.8 s)
#  change POST to 1.5
# mk_timing.bash:
#    PRE_TIME=.5
#    POST_TIME=1.5
# TODO: up firstiti to 3.233 (replacing PRE_TIME=.5)
#  
# but combining with 3s at start of each
# where does 5-1.5 above come from?
#   3*3 added as first iti, keeping .5*3 on top but losing another 1.5 on bottom
#  afni_to_task.R: FIRSTITI <- 3 # in seconds
VINFO="-nocatch-qwalk"

MAXJOBS=10
source /opt/ni_tools/lncdshell/utils/waitforjobs.sh
set -euo pipefail

for i in $(seq 0 $NSIMS); do
   ./mk_timing.bash $NTRIALS $TOTAL_DUR $MINITI v${MINITI}${VINFO} &
   waitforjobs
done
wait
