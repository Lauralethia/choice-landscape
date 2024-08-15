#!/usr/bin/env bash
#
# quickly read example task output from LAF
# 20240815WF - init
#
cd "$(dirname "$0")"
source ../read_idv_task.bash # get add_info
cat 20240815_LAF_1722548170258.json |
   add_info LAF 20240815 eeg NA |
   ../dbjsontotsv.jq  |
   tee LAF_raw.tsv| 
   Rscript -e 'source("../read.R");read_task_stdin()' | 
   tee LAF.tsv
