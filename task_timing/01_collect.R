#!/usr/bin/env Rscript

# 20220510WF - init
#  collect all std dev norm tests. lowest is best
suppressPackageStartupMessages(library(dplyr))
source('funcs.R')
max_catch <- function(f) read_timing(f) %>% timing_mat %>% filter(prop=='catch',value==TRUE) %$% nrep %>% max

# read events. get max-in-a-row
read_all <- function(gen_ver="v1", total_dur=240) {
   events <- Sys.glob(paste0('out/',total_dur,'s/',gen_ver,'_*/events.txt'))

   catches <- sapply(events, max_catch) # takes a few minutes
   catches_df <- data.frame(name=cname(events,tt=total_dur, v=gen_ver), maxcatch=catches)

   # read GLM
   run_std <- Sys.glob(paste0('out/',total_dur,'s/',gen_ver,'_*/stddevtests.tsv')) %>% lapply(read.table,header=T) %>% bind_rows
   d <- left_join(catches_df, run_std)
   write.table(d, paste0(total_dur,'_',gen_ver,'_std_dev_tests.tsv'), row.names=F)
   return(d)
}

simplify_lc <- function(d, max_rep_catch=2) {
 d %>%
    filter(maxcatch < max_rep_catch) %>% # first instance is nrep == 0
    #arrange(choice_LC) %>%
    select(name,choice_LC,choice.fbk_LC, good.nogood_LC, g_fbk.ng_fbk_LC) %>%
    rowwise() %>% mutate(sum_LC = sum(c_across(-name))) %>% ungroup() %>%
    arrange(sum_LC)
}

rank_LC <- function(d, sort_col) {
 d %>%
    simplify_lc() %>% select(-sum_LC) %>%
    mutate(across(matches("_LC"), rank)) %>%
    #mutate(across(matches("_LC"), function(x) scale(x-min(x), center=F))) %>%
    rowwise() %>% mutate(overall = sum(c_across(-name)))
}

### pick
d_rank <- read_all(gen_ver="v1", total_dur=280) %>%  rank_LC()
# show
#d_rank %>% arrange(choice.fbk_LC)  %>%  head %>% print
d_rank %>% arrange(overall) %>% head %>% print.data.frame(row.names=F)

# library(ggplot2)
# theme_set(cowplot::theme_cowplot())
# ggplot(d) + aes(x=choice_LC, y=choice.fbk_LC) + geom_point() + geom_smooth()

## inspect differences of min(iti) and total_duration
head_miniti<-function(v, miniti, dur="240", n=Inf) read.table(paste0(dur,'_',v,'_std_dev_tests.tsv'), header=T) %>% simplify_lc() %>% head(n=n) %>% mutate(miniti=miniti, totaldur=dur)

rbind(head_miniti("v025",.25),
      head_miniti("v1",1.0),
      head_miniti("v1.5",1.5),
      head_miniti("v1", 1.0, 280),
      head_miniti("v1.5", 1.5, 280)) %>%
  mutate(miniti=as.factor(miniti)) %>%
  ggplot() +
   aes(x=choice_LC, y=choice.fbk_LC, size=sum_LC, color=miniti) +
   geom_point(alpha=.5) +
   ggtitle("min(iti) LCs")+
   facet_wrap(~totaldur)

rbind(head_miniti("v1",1.0, 280),
      head_miniti("v1.5",1.5, 280)) %>%
  mutate(miniti=as.factor(miniti)) %>%
  ggplot() + aes(x=choice_LC, y=choice.fbk_LC, size=sum_LC, color=miniti) + geom_point(alpha=.5) +
  ggtitle("min(iti) LCs (280s)")
