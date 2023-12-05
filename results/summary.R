#!/usr/bin/env Rscript
# summarise mturk/herokuapp into row per run
# pull in survey results and key presses
 
library(dplyr)
library(ggplot2)
library(lubridate)
theme_set(cowplot::theme_cowplot())

MINDATE <- ymd('2022-04-10')
VERSION_FILTER_REGEX <- "v10"    # used for plotting
summarise_habit <- function(surveyf="survey.tsv", dataf="data.tsv", outfile="summary.csv",
                            mindate=MINDATE) {
  
  # from extra_info.jq (run by Makefile)
  survey <- read.table(surveyf, sep="\t",header=T,comment.char="",quote="") %>%
      mutate(vdate=ymd_hms(vdate), age=as.numeric(age)) # numeric b/c some are empty. one student reported age="I 8"

  # from ./read.R
  d <- read.csv(file=dataf, sep="\t") %>% mutate(vdate=ymd_hms(vdate)) %>%
    # x is junk ID used for testing
    filter(! id %in% c("x"))
  
  # remove "rnd" later. maybe filter(vdate>"2022-02-24") instead
  #d_rcnt <- d %>% filter(grepl("^\\d{5}", id)) # lunaids; also restrict to version: grepl("v9", ver), 
  d_rcnt <- d %>% mutate(age=as.numeric(age)) #%>% filter(vdate > MINDATE)
  
  
  # quick metrics for assessing performance
  # 'score' includes luck. could pick 20% side and still get a point or pick 50 in 20/50 without reward
  # 'optimal' = 1 if you knew exactly when the blocktype switch was
  # 'train_*' is for init and switch block. 'deval_*' is for deval only blocks
  # 'dur' is in minutes
  smry <- d_rcnt %>% group_by(id,ver,vdate,age) %>%
      summarise(n=n(),
                rt_mean=mean(rt,na.rm=T),
                rt_sd=sd(rt,na.rm=T),
                score=sum(score=="true",na.rm=T), 
                optimal=sum(optimal_choice,na.rm=T)/n,
                n_miss=length(which(is.na(rt))),
                n_keys_mean=mean(n_keys,na.rm=T),
                first_key_mean=mean(first_key_rt,na.rm=T),
                train_n_good=sum(pick_dist=="far"&!grepl("devalue",blocktype),na.rm=T),
                train_n_avoidfar=sum(avoid_unified=="first100"&!grepl("devalue",blocktype),na.rm=T),
                deval100_n_far=sum(pick_dist=="far"&blocktype=="devalue_all_100",na.rm=T),
                deval100_n_avoidfar=sum(avoid_unified=="first100"&blocktype=="devalue_all_100",na.rm=T),
                dur=(max(iti_onset)-min(iti_onset))/(60*10^3))
  
  smry_fbk <- left_join(smry, survey)
  write.csv(smry_fbk, outfile, row.names=F, quote=T)
}
   
plot_summary <- function(d_rcnt, version_filter=VERSION_FILTER_REGEX) {
   ## plot all trials. facet by id (currently, id is unique to each run)
   # color rectanges for block switches. hard coding first100 and first50 b/c '*unified' columns could have NA
   d_facet_id <- d_rcnt %>%
       mutate(shortver=substr(gsub('.*(v[0-9]+).*','\\1',ver),0,10),
              shortid=substr(id,0,7),
              facet=paste(age,shortver,id)) %>%
              filter(grepl(version_filter,facet)) 
   
   blocks <- d_facet_id %>%
       group_by(facet,blocktype) %>%
       summarise(xmin=min(trial), xmax=max(trial),ymin="first100",ymax=max(d_rcnt$avoid_unified,na.rm=T)) # "first50" but maybe first90
   
   all_plots <- d_facet_id %>% 
   ggplot() +
       aes(x=trial) +
       geom_rect(data=blocks, aes(x=NULL,ymax=ymax,ymin=ymin,xmax=xmax, xmin=xmin, fill=blocktype), alpha=.2) +
       geom_point(aes(y=avoid_unified),color='gray') +
       geom_point(aes(y=picked_unified,color=optimal_choice,shape=score)) +
       facet_grid(facet~.) +
       scale_shape_manual(values=c(4,5,20)) +
       ylab('side') + theme(strip.text.x = element_text(hjust = -0.02))
   print(all_plots)
}

if (sys.nframe() == 0){
   cargs <- commandArgs(trailingOnly=TRUE)
   if(length(cargs)<3L || length(cargs)>4)
      stop(glue::glue(
   "ERROR: given {length(cargs)} args. want between 3 and 4
   USAGE:
     ./summary.R info.tsv data.tsv summary.csv [mindate={MINDATE}]"))
   do.call(summarise_habit, as.list(cargs))
}
