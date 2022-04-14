#!/usr/bin/env Rscript
# 20220412FC - init copied in (WF)
if (!require("pacman")) install.packages("pacman")
pacman::p_load(shiny, shinydashboard, tidyr, dplyr,
               readr, DT, pracma, XML, ggplot2, zoo, 
               tractor.base, jsonlite, sjPlot, tidyquant,
               mgcv, mgcViz, scam, forcats)
select <- dplyr::select

# narrow data to just more recent versions
# and exclude testing runs (IDs with our initials, or 'x')
VER_REGEX <- 'v9_|v10_'
BAD_IDS <- c("WWF|ACP|^x$")
MIN_TRIALS <- 120

rawdata <- read.csv('data.tsv', sep='\t') %>% 
  filter(grepl(VER_REGEX,ver) & !grepl(BAD_IDS, id))
  #filter(id != 'WWF34M' & id != 'ACP34F' & id != 'AP' & id != 'FC' & ver == '20211104v4-90max_pbar_moredeval')
  #filter(ver == '20211025v3-longertrials')

rawdata %>% select(id, survey_age, vdate, ver) %>% distinct()

# make sure we have enough trials to use
total_trials <- rawdata %>%
    group_by(id, vdate, age=survey_age) %>%
    summarize(ntrials = max(trial)) %>%
    filter(ntrials > MIN_TRIALS)

# set fawWell and initHigh side: 1 (left) ,2 (up), or 3 (right)
farProb <- max(rawdata$up_prob) # expect 100. but might be 95
first_trial_wellnames <- rawdata %>% filter(trial == 1) %>% 
    mutate(
        # best (was at one time far, likely all same distance now) 
        farWell = ifelse(left_prob == farProb, 1, ifelse(up_prob==farProb, 2, ifelse(right_prob==farProb, 3, NA))),
        # side that was first high (likely of 50/20) 
        initHigh = ifelse(left_prob == 50, 1, ifelse(up_prob==50, 2, 3))) %>%
    select(id, farWell, initHigh)

perSubj <- merge(total_trials, first_trial_wellnames, by='id')

#
#        farWell_x = case_when(left_prob == farProb ~ 1,
#                              up_prob == farProb ~ 2,
#                              right_prob==farProb ~ 3,
#                              TRUE ~ NA),
#        initHigh_x = case_when(left_prob == 50 ~ 1,
#                             up_prob   == 50 ~ 2,
#                             TRUE ~ 3),
#

# probably could do with as.factor and keep labels or use case_when.
# blocktype needs additional attention. have more names
# numbers only needed to compare to farWell and initHigh.
# can maybe save some effort and use string values directly?
# blocknum only used to display block rectangles
side_label_to_num <- function(side) ifelse(side == 'left', 1, ifelse(side=='up', 2, ifelse(side=='right', 3, NA)))
blocktype_to_num <- function(blocktype)
    ifelse(   blocktype == 'init',                1,
     ifelse(  blocktype == 'switch1',             2,
      ifelse( blocktype %in% c("rev2","switch2"), 3,
       ifelse(grepl('devalue', blocktype),        4,
                                                  NA))))

# make sure we get the blocks correctly
test_blocktypenum<-function(){
    testthat::expect_equal(blocktype_to_num('switch1'), 2)
    testthat::expect_equal(blocktype_to_num('devalue_all_100'), 4)
    testthat::expect_equal(blocktype_to_num('devalue_good_75'), 4)
    testthat::expect_equal(blocktype_to_num(c('switch1','devalue_all_low')), c(2,4))
    testthat::expect_false(rawdata$blocktype %>% unique %>% blocktype_to_num %>% is.na %>% any)
}

# have various combinations of blocks
#   init-switch1-devalue_all_100
#   init-switch1-devalue_all_100-devalue_all_low
blockseq_df <-
    rawdata %>% group_by(id,ver,timepoint) %>%
    arrange(trial) %>%
    summarise(blockseq=paste0(unique(blocktype),collapse="-"))

data <-
  rawdata %>% filter(id %in% perSubj$id) %>%
  merge(perSubj, by=c('id','vdate')) %>%
  left_join(blockseq_df, by=c("id","ver","timepoint")) %>%
  mutate(
    choiceWell  = side_label_to_num(picked),
    avoidedWell = side_label_to_num(avoided),
    # what wells be chosen
    farAvailable = choiceWell == farWell | avoidedWell == farWell,
    initHighAval = choiceWell == initHigh | avoidedWell == initHigh,
    # only care about high and best decisions
    # NA needed for subsetting? cant use e.g.: choseFar = farAvailable & choiceWell == farWell,
    choseFar        = ifelse(farAvailable, choiceWell == farWell, NA),
    avoidedFar      = ifelse(farAvailable, avoidedWell == farWell, NA),
    choseInitHigh   = ifelse(initHighAval, choiceWell == initHigh,NA),
    avoidedInitHigh = ifelse(initHighAval, avoidedWell == initHigh,NA),
    # 
    blocknum = blocktype_to_num(blocktype),
    choiceType = ifelse(true.na(choseFar), 'Far', ifelse(true.na(choseInitHigh), 'InitHigh', 'InitLow')))

#### PLOTTING
MAXTRIALS <- max(data$trial)

geom_block_rect <- function(d, gby, ylim=c(0,4)) {
    # gby should be at least var('blocktype')
    blocks <- d %>%
        group_by_at(gby) %>%
        summarise(xmin=min(trial), xmax=max(trial),
                  ymin=ylim[1], ymax=ylim[2])
    geom_rect(data=blocks, aes(x=NULL, color=NULL, shape=NULL, size=NULL, y=NULL,
                               ymax=ymax,ymin=ymin,xmax=xmax, xmin=xmin, fill=blocktype), alpha=.2)
}



# optimal choices during learning phase
no_deval <- data %>% filter(!is.na(optimal_choice) & !grepl('devalue', blocktype))
ggplot(no_deval) +
    aes(x=trial, y=1*optimal_choice, group=id, color=id) + 
    geom_block_rect(no_deval, vars('id','blocktype')) +
    geom_point() + 
    geom_ma(n = 10, ma_fun = EMA, color = "red", linetype=1) + 
    facet_wrap(facets = vars(id))

# preference for far well by block
far_only <- data %>% filter(!is.na(choseFar))  %>% arrange(trial)
ggplot(far_only)+
  aes(x=trial, y=1*choseFar, group=id, color=id) + 
  geom_block_rect(far_only, vars('id','blocktype')) +
  geom_point() + 
  geom_ma(n = 10, ma_fun = EMA, color = "red", linetype=1) + 
  facet_wrap(facets = vars(id), ncol = 4) +
  coord_cartesian(xlim = c(1,MAXTRIALS)) +
  theme(legend.position = 'none')

# reversal learning (trials excluded far well as a choice)
no_far <- data %>% filter(is.na(choseFar)) %>% arrange(trial)
ggplot(no_far)+
  aes(x=trial, y=1*choseInitHigh, group=id, color=id) + 
  geom_block_rect(no_far, vars('id','blocktype')) +
  geom_point() + 
  geom_ma(n = 5, ma_fun = EMA, color = "red", linetype=1) + 
  facet_wrap(facets = vars(id), ncol = 4) +
  coord_cartesian(xlim = c(1,MAXTRIALS)) +
  theme(legend.position = 'none')


# group average learning
ggplot(no_far) +
  aes(x=trial, y=1*choseInitHigh) + 
  geom_block_rect(no_far, vars('blocktype','blockseq')) +
  geom_ma(n = 50, ma_fun = EMA, color = "red", linetype=1) + 
  coord_cartesian(xlim = c(1,MAXTRIALS), ylim = c(0,1)) +
  theme(legend.position = 'none') +
  facet_wrap(~blockseq)

# group average - far well
ggplot(far_only) +
  aes(x=trial, y=1*choseFar) + 
  geom_block_rect(far_only, vars('blocktype','blockseq')) +
  stat_smooth(span=0.1) +
  coord_cartesian(xlim = c(1,MAXTRIALS), ylim = c(0,1)) +
  facet_wrap(~blockseq) +
  theme(legend.position = 'none')

# group average - far well, + indiv traces
ggplot(far_only) +
  aes(x=trial, y=1*choseFar) + 
  geom_block_rect(far_only, vars('blocktype','blockseq')) +
  stat_smooth(aes(group=id), se=F, span=1.5, color='gray', method='loess') +
  stat_smooth(span=0.1, se=T) +
  coord_cartesian(xlim = c(1,MAXTRIALS), ylim = c(0,1)) +
  facet_wrap(~blockseq) +
  theme(legend.position = 'none')

# group average - far well, + indiv traces, moving avg
ggplot(far_only)+
  aes(x=trial, y=1*choseFar) + 
  geom_block_rect(far_only, vars('blocktype','blockseq')) +
  geom_ma(n = 20, ma_fun = ZLEMA, linetype=1, aes(group=id), color='gray') + 
  geom_ma(n = 150, ma_fun = ZLEMA, linetype=1, color='blue') + 
  coord_cartesian(xlim = c(1,MAXTRIALS), ylim = c(0,1)) +
  facet_wrap(~blockseq) +
  theme(legend.position = 'none')

data %>% group_by(id) %>% summarize(n = n()) %>% group_by(n) %>% tally()


no_far_no_deval <- data %>% filter(is.na(choseFar) & !grepl('devalue',blocktype)) %>% arrange(trial)
ggplot(no_far_no_deval) +
  aes(x=trial, y=1*optimal_choice) + 
  geom_block_rect(no_far_no_deval, vars('blocktype','blockseq')) +
  geom_ma(n = 25, ma_fun = EMA, color = "red", linetype=1) + 
  coord_cartesian(xlim = c(1,MAXTRIALS), ylim = c(0,1)) +
  facet_wrap(~blockseq) +
  theme(legend.position = 'none')


# group average - RT
rt_data <- data %>% filter(!is.na(rt)) %>% arrange(trial)
ggplot(rt_data) + aes(x=trial, y=rt, color=as.factor(choiceType)) + 
  geom_block_rect(rt_data, vars('blocktype','blockseq'), ylim=c(400,800)) +
  stat_smooth(span = 0.5) +
  coord_cartesian(xlim = c(1,MAXTRIALS), ylim = c(400,800)) +
  facet_wrap(~blockseq) +
  theme(legend.position = 'top')


data %>% group_by(id, blocktype) %>% summarize(choseFar = sum(choseFar, na.rm=T), avoidedFar = sum(avoidedFar, na.rm=T), pChoseFar = choseFar / (choseFar + avoidedFar)) %>%
  #mutate(blocktype = fct_relevel(blocktype, c('init','switch1','rev2','devalue'))) %>%
  group_by(blocktype) %>% summarize(pChoseFar = mean(pChoseFar, na.rm=T), pChoseFar_sd = sd(pChoseFar, na.rm=T), pChoseFar_se = sd(pChoseFar, na.rm=T)/sqrt(n()), n=n())  
 

# compute % choseFar in final block, plot vs age
habitBeh <- data %>%
    filter(trial > 140) %>%
    group_by(id, age.x, age.y, blockseq, task) %>% 
  summarize(pHabit = sum(choseFar, na.rm=T) /
                    (sum(choseFar, na.rm=T) + sum(avoidedFar, na.rm=T)))

ggplot(data=habitBeh %>% filter(age.x < 50 & age.x > 18))+
 aes(x=age.x, y=pHabit) +
 geom_point(aes(color=blockseq, shape=as.factor(substr(task,0,5)))) +
 stat_smooth(method='loess') +
 coord_cartesian(ylim=c(0,1)) +
 theme(legend.position = 'bottom')
 

ggplot(habitBeh) + aes(age.x) + geom_histogram()
