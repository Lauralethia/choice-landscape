#!/usr/bin/env bash
NSIMS=1000
NTRIALS=53
#MINITI=1.5
MINITI=1
# 12min (720s) of 3 concatinted runs 
#TOTAL_DUR=240
# 14m 
TOTAL_DUR=280
for i in $(seq 0 $NSIMS); do
   bq ./mk_timing.bash $NTRIALS $TOTAL_DUR $MINITI
done
