#!/usr/bin/env bash
NSIMS=1000
NTRIALS=102
TOTAL_DUR=500
for i in $(seq 0 $NSIMS); do
   bq ./mk_timing.bash $NTRIALS $TOTAL_DUR
done
