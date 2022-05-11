#!/usr/bin/env Rscript

# 20220510WF - init
#  collect all std dev norm tests. lowest is best
suppressPackageStartupMessages(library(dplyr))
d <- Sys.glob('out/*/*/stddevtests.tsv') %>% lapply(read.table,header=T) %>% bind_rows
write.table(d, 'std_dev_tests.tsv', row.names=F)



