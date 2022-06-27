#!/usr/bin/env Rscript

# 20220510WF - init
#  collect all std dev norm tests. lowest is best
suppressPackageStartupMessages(library(dplyr))
source('funcs.R')
max_catch <- function(f) read_timing(f) %>% timing_mat %>% filter(prop=='catch',value==TRUE) %$% nrep %>% max

# read events. get max-in-a-row
events <- Sys.glob('out/240s/v1*/events.txt')

# "v1_53_1012" to "v1-240-53-1012"
cname <- function(f, tt="240",v="v1") paste(sep="-", v, tt, gsub("_","-", gsub(paste0("^",v,"_"),"",basename(dirname(f)))))

catches <- sapply(events, max_catch) # takes a few minutes
catches_df <- data.frame(name=cname(events), maxcatch=catches)

# read GLM
run_std <- Sys.glob('out/240s/v1*/stddevtests.tsv') %>% lapply(read.table,header=T) %>% bind_rows
d <- left_join(catches_df, run_std)
write.table(d, '240_std_dev_tests.tsv', row.names=F)
# d <- read.table('240_std_dev_tests.tsv', header=T)

find_best <- function(d, sort_col, max_rep_catch=2) {
 d %>%
    filter(maxcatch < max_rep_catch) %>% # first instance is nrep == 0
    #arrange(choice_LC) %>%
    select(name,choice_LC,choice.fbk_LC, good.nogood_LC, g_fbk.ng_fbk_LC) %>%
    mutate(across(matches("_LC"), rank)) %>%
    #mutate(across(matches("_LC"), function(x) scale(x-min(x), center=F))) %>%
    rowwise() %>% mutate(overall = sum(c_across(-name))) %>%
    arrange({{sort_col}})
}
d %>% find_best(choice.fbk_LC)  %>%  head %>% print
d %>% find_best(overall)  %>%  head %>% print

# library(ggplot2)
# theme_set(cowplot::theme_cowplot())
# ggplot(d) + aes(x=choice_LC, y=choice.fbk_LC) + geom_point() + geom_smooth()
