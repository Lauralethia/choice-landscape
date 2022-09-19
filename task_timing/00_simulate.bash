#!/usr/bin/env bash
NSIMS=1000
NTRIALS=34  # x3 => 102 trials
NTRIALS=45  # x3 =>  trials
MINITI=1.5  # 1
# running 1 and 1.5 to check timing is not a constraint
TOTAL_DUR=185 # x3 => 9.25 minutes
#TOTAL_DUR=134 # x3 (+3 first) (+ 5 -1.5 last iti)=>
VINFO="-nocatch-qwalk"

MAXJOBS=10
source /opt/ni_tools/lncdshell/utils/waitforjobs.sh
set -euo pipefail

for i in $(seq 0 $NSIMS); do
   ./mk_timing.bash $NTRIALS $TOTAL_DUR $MINITI v${MINITI}${VINFO} &
   waitforjobs
done
