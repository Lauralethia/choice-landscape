library(dplyr)
library(ggplot2)
library(lubridate)
theme_set(cowplot::theme_cowplot())

d <- read.csv(file="data.tsv", sep="\t") %>% mutate(vdate=ymd_hms(vdate))
# remove "rnd" later. maybe filter(vdate>"2022-02-24") instead
#d_rcnt <- d %>% filter(grepl("^\\d{5}", id)) # lunaids; also restrict to version: grepl("v9", ver), 
d_rcnt <- d  %>% filter(vdate > ymd('2022-01-01'), age>5, age<90)

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
              train_n_far=sum(pick_dist=="far"&blocktype!="devalue",na.rm=T),
              train_n_avoidfar=sum(avoid_unified=="first100"&blocktype!="devalue",na.rm=T),
              deval_n_far=sum(pick_dist=="far"&blocktype=="devalue",na.rm=T),
              deval_n_avoidfar=sum(avoid_unified=="first100"&blocktype=="devalue",na.rm=T),
              dur=(max(iti_onset)-min(iti_onset))/(60*10^3))
write.csv(smry, 'summary.csv', row.names=F, quote=F)
   
## plot all trials. facet by id (currently, id is unique to each run)
# color rectanges for block switches. hard coding first100 and first50 b/c '*unified' columns could have NA
d_facet_id <- d_rcnt %>%
    mutate(shortver=substr(gsub('.*(v[0-9]+).*','\\1',ver),0,10),
           shortid=substr(id,0,7),
           facet=paste(age,shortver,id)) %>%
           filter(grepl('v9',facet)) 

blocks <- d_facet_id %>%
    group_by(facet,blocktype) %>%
    summarise(xmin=min(trial), xmax=max(trial),ymin="first100",ymax=max(d_rcnt$avoid_unified,na.rm=T)) # "first50" but maybe first90

all_plots <- d_facet_id %>% 
ggplot() +
    aes(x=trial) +
    geom_rect(data=blocks, aes(x=NULL,ymax=ymax,ymin=ymin,xmax=xmax, xmin=xmin, fill=blocktype), alpha=.2) +
    geom_point(aes(y=avoid_unified),color='gray') +
    geom_point(aes(y=picked_unified,color=optimal_choice)) +
    facet_wrap(~facet) +
    ylab('side') + theme(strip.text.x = element_text(hjust = -0.02))
print(all_plots)
