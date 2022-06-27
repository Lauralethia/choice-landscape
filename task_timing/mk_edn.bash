#!/usr/bin/env bash
# 20220617WF - init
# output should go into ../src/landscape/fixed_timing.cljs as named counterbalence

# originall looking at
#out/240s/v1_53_31634/events.txt out/240s/v1_53_20017/events.txt out/240s/v1_53_19352/events.txt
# but too many catches in a row

# hand picked from 01_collect.R and visualize_timing.R
#./afni_to_task.R out/240s/v1_53_{6156,23263,1599}/events.txt
./afni_to_task.R out/280s/v1.5_53_{10987,32226,24271}/events.txt

