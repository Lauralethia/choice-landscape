#!/usr/bin/env Rscript

# 20220510WF - init
#  collect all std dev norm tests. lowest is best
suppressPackageStartupMessages(library(dplyr))
d <- Sys.glob('out/*/*/stddevtests.tsv') %>% lapply(read.table,header=T) %>% bind_rows
write.table(d, 'std_dev_tests.tsv', row.names=F)

d[head(order(d$choice_LC)),c("name","choice_LC","choice.fbk_LC","good.nogood_LC","g_fbk.ng_fbk_LC")] %>% print


