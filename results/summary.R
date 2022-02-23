library(dplyr)
library(ggplot2)
library(lubridate)
theme_set(cowplot::theme_cowplot())

d <- read.csv(file="data.tsv", sep="\t") %>% mutate(vdate=ymd_hms(vdate))
# remove "rnd" later. maybe filter(vdate>"2022-02-24") instead
d_rcnt <- d %>% filter(grepl("v8", ver), grepl("rnd", id), age<99)

# quick metrics for assessing performance
# 'score' includes luck. could pick 20% side and still get a point or pick 50 in 20/50 without reward
# 'optimal' = 1 if you knew exactly when the blocktype switch was
# 'train_*' is for init and switch block. 'deval_*' is for deval only blocks
# 'dur' is in minutes
smry <- d_rcnt %>% group_by(id,vdate,age) %>%
    summarise(n=n(),
              rt_mean=mean(rt,na.rm=T),
              rt_sd=sd(rt,na.rm=T),
              score=sum(score=="true",na.rm=T), 
              optimal=sum(optimal_choice,na.rm=T)/n,
              n_miss=length(which(is.na(rt))),
              train_n_far=sum(pick_dist=="far"&blocktype!="devalue",na.rm=T),
              train_n_avoidfar=sum(avoid_unified=="first100"&blocktype!="devalue",na.rm=T),
              deval_n_far=sum(pick_dist=="far"&blocktype=="devalue",na.rm=T),
              deval_n_avoidfar=sum(avoid_unified=="first100"&blocktype=="devalue",na.rm=T),
              dur=(max(iti_onset)-min(iti_onset))/(60*10^3))
write.csv(smry, 'summary.csv', row.names=F, quote=F)
   
## plot all trials. facet by id (currently, id is unique to each run)
# color rectanges for block switches. hard coding first100 and first50 b/c '*unified' columns could have NA
blocks <- d_rcnt %>%
    group_by(id,blocktype) %>%
    summarise(xmin=min(trial), xmax=max(trial),ymin="first100",ymax="first50")

d_rcnt %>%
    ggplot() +
    aes(x=trial) +
    geom_point(aes(y=avoid_unified),color='gray') +
    geom_point(aes(y=picked_unified,color=optimal_choice)) +
    geom_rect(data=blocks, aes(x=NULL,ymax=ymax,ymin=ymin,xmax=xmax, xmin=xmin, fill=blocktype), alpha=.1) +
    facet_grid(id~.) +
    ylab('side')
