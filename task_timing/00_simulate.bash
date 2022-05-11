#!/usr/bin/env bash
NSIMS=10
NTRIALS=100
TOTAL_DUR=500
for i in $(seq 0 $NSIMS); do
   ./mk_timing.bash $NTRIALS $TOTAL_DUR
done
