#!/usr/bin/env bash
# 20220617WF - init
# output should go into ../src/landscape/fixed_timing.cljs as named counterbalence

# originall looking at
#out/240s/v1_53_31634/events.txt out/240s/v1_53_20017/events.txt out/240s/v1_53_19352/events.txt
# but too many catches in a row

# hand picked from 01_collect.R and visualize_timing.R
#./afni_to_task.R out/240s/v1_53_{6156,23263,1599}/events.txt
test -d edn || mkdir "$_"

# original with catch trials
# ./afni_to_task.R out/280s/v1.5_53_{10987,32226,24271}/events.txt > edn/mra1.edn
# ./afni_to_task.R out/280s/v1.5_53_{23960,28898,25862}/events.txt > edn/mra2.edn
# # TODO: use different seeds?
# ./afni_to_task.R --left out/280s/v1.5_53_{10987,32226,24271}/events.txt > edn/mrb1.edn
# ./afni_to_task.R --left out/280s/v1.5_53_{23960,28898,25862}/events.txt > edn/mrb2.edn

# without catches
# d<-read.table('./185_v1.5-nocatch_std_dev_tests.tsv',header=T)
# slc <- simplify_lc(d)
# inner_join(slc %>% arrange(choice_LC) %>% head(n=50),slc %>% arrange(good.nogood_LC) %>% head(n=50)) %>% showdf
#  name choice_LC choice.fbk_LC good.nogood_LC g_fbk.ng_fbk_LC sum_LC
# 22353    2.8978        1.4732         2.5402          2.7186 9.6298
#  7403    2.9229        1.4598         2.5729          2.7544 9.7100
# 26746    2.9308        1.4529         2.5763          2.7802 9.7402
#
# inner_join(slc %>% arrange(choice_LC) %>% head(n=70),slc %>% arrange(g_fbk.ng_fbk_LC) %>% head(n=70)) %>% showdf
#   name choice_LC choice.fbk_LC good.nogood_LC g_fbk.ng_fbk_LC sum_LC
# 22353    2.8978        1.4732         2.5402          2.7186 9.6298
#  7403    2.9229        1.4598         2.5729          2.7544 9.7100
# 28744    2.9445        1.4365         2.5841          2.7637 9.7288
# 23677    2.9471        1.4510         2.5858          2.7673 9.7512
# 31668    2.9886        1.4480         2.5715          2.7528 9.7609

# choice.fbk_LC range is already low and not imporoved much by direct sort. 
# but does pull choice_LC far down
#> slc %>% arrange(choice.fbk_LC) %>% head(15) %>% arrange(choice_LC) %>% showdf
#                      name choice_LC choice.fbk_LC good.nogood_LC g_fbk.ng_fbk_LC  sum_LC
# v1.5-nocatch-185-34-29041    3.2445        1.3813         2.6452          2.8400 10.1110
#  v1.5-nocatch-185-34-6669    3.2746        1.3817         2.8000          2.9895 10.4458
#  v1.5-nocatch-185-34-3026    3.3002        1.3815         2.6545          2.8551 10.1913

files=( out/185s/v1.5-nocatch_34_{22353,7403,28744,23677,31668}/events.txt )

./afni_to_task.R "${files[@]:0:3}" > edn/mr_nocatch_1_right.edn
./afni_to_task.R "${files[@]:2:3}" > edn/mr_nocatch_2_right.edn
./afni_to_task.R --left "${files[@]:0:3}" > edn/mr1_nocatch_1_left.edn
./afni_to_task.R --left "${files[@]:2:3}" > edn/mr2_nocatch_1_left.edn

