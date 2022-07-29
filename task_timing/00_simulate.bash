#!/usr/bin/env bash
NSIMS=1000
NTRIALS=34  # x3 => 102 trials
MINITI=1.5  # 1
# running 1 and 1.5 to check timing is not a constraint
TOTAL_DUR=185 # x3 => 9.25 minutes
for i in $(seq 0 $NSIMS); do
   bq ./mk_timing.bash $NTRIALS $TOTAL_DUR $MINITI v${MINITI}-nocatch
done
