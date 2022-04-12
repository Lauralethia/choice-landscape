#!/usr/bin/env Rscript
# 20220412FC - init copied in (WF)
if (!require("pacman")) install.packages("pacman")
pacman::p_load(shiny, shinydashboard, tidyr, dplyr,
               readr, DT, pracma, XML, ggplot2, zoo, 
               tractor.base, jsonlite, sjPlot, tidyquant,
               mgcv, mgcViz, scam, forcats)


setwd('~/scratch/habit')
read.csv('data.tsv', sep='\t') %>% select(ver, id) %>% distinct() %>% group_by(ver) %>% tally()
rawdata <- read.csv('data.tsv', sep='\t') %>% 
  filter(ver == '20220228v9_goodclose' & !grepl('WWF', id) & !grepl('ACP', id))
  #filter(id != 'WWF34M' & id != 'ACP34F' & id != 'AP' & id != 'FC' & ver == '20211104v4-90max_pbar_moredeval')
  #filter(ver == '20211025v3-longertrials')

farProb <- max(rawdata$up_prob)
names(rawdata)
rawdata %>% dplyr::select(id, survey_age, vdate, ver) %>% distinct()
rawdata %>% filter(id == 'rndZVU_621570cf') %>% 
  dplyr::select(trial, left_prob, up_prob, right_prob, block) %>%
  group_by(block) %>% tally()


perSubj <- merge(rawdata %>% group_by(id, vdate, age=survey_age) %>% summarize(ntrials = max(trial)) %>% filter(ntrials > 120),
                 rawdata %>% filter(trial == 1) %>% 
                   mutate(
                     farWell = ifelse(left_prob == farProb, 1, ifelse(up_prob==farProb, 2, ifelse(right_prob==farProb, 3, NA))),
                     initHigh = ifelse(left_prob == 50, 1, ifelse(up_prob==50, 2, 3))
                   ) %>% dplyr::select(id, farWell, initHigh),
                 by='id')


#rawdata %>% filter(id=='FC' & ver == '20211104v4-90max_pbar_moredeval') %>% select(trial, left_prob, up_prob, right_prob, picked, avoided)
#perSubj %>% filter(id=='FC')

data <- merge(rawdata %>% filter(id %in% perSubj$id), 
              perSubj, 
              by=c('id','vdate')) %>%
  mutate(
    choiceWell = ifelse(picked == 'left', 1, ifelse(picked=='up', 2, ifelse(picked=='right', 3, NA))),
    avoidedWell = ifelse(avoided == 'left', 1, ifelse(avoided=='up', 2, ifelse(avoided=='right', 3, NA))),
    choseFar = ifelse(choiceWell == farWell | avoidedWell == farWell, choiceWell == farWell, NA),
    avoidedFar = ifelse(choiceWell == farWell | avoidedWell == farWell, avoidedWell == farWell, NA),
    choseInitHigh = ifelse(choiceWell == initHigh | avoidedWell == initHigh, choiceWell == initHigh, NA),
    avoidedInitHigh = ifelse(choiceWell == initHigh | avoidedWell == initHigh, avoidedWell == initHigh, NA),
    blocknum = ifelse(blocktype == 'init', 1, ifelse(blocktype == 'switch1', 2, ifelse(blocktype == 'rev2', 3, ifelse(blocktype == 'devalue', 4, NA)))),
    choiceType = ifelse(!is.na(choseFar) & choseFar == 1, 'Far', ifelse(!is.na(choseInitHigh) & choseInitHigh == 1, 'InitHigh', 'InitLow'))
  )

# optimal chocies during learning phase
ggplot(data = data %>% filter(!is.na(optimal_choice) & blocktype != 'devalue'), aes(x=trial, y=1*optimal_choice, group=id, color=id)) + 
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.01, color='white') +
  geom_point() + 
  geom_ma(n = 10, ma_fun = EMA, color = "red", linetype=1) + 
  facet_wrap(facets = vars(id))

# preference for far well by block
ggplot(data = data %>% filter(!is.na(choseFar)), aes(x=trial, y=1*choseFar, group=id, color=id)) + 
  geom_point() + 
  geom_ma(n = 10, ma_fun = EMA, color = "red", linetype=1) + 
  facet_wrap(facets = vars(id), ncol = 4) +
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.01, color='white') +
  coord_cartesian(xlim = c(1,204)) + theme(legend.position = 'none')

# reversal learning (trials excluded far well as a choice)
ggplot(data = data %>% filter(is.na(choseFar)), aes(x=trial, y=1*choseInitHigh, group=id, color=id)) + 
  geom_point() + 
  facet_wrap(facets = vars(id), ncol = 4) +
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.01, color='white') +
  geom_ma(n = 5, ma_fun = EMA, color = "red", linetype=1) + 
  coord_cartesian(xlim = c(1,204)) + theme(legend.position = 'none')


# group average learning
ggplot(data = data %>% filter(is.na(choseFar)) %>% arrange(trial), aes(x=trial, y=1*choseInitHigh)) + 
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.00, color='black') +
  geom_ma(n = 50, ma_fun = EMA, color = "red", linetype=1) + 
  coord_cartesian(xlim = c(1,204), ylim = c(0,1)) + theme(legend.position = 'none')

# group average - far well
ggplot(data = data %>% filter(!is.na(choseFar)) %>% arrange(trial), aes(x=trial, y=1*choseFar)) + 
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.00, color='black') +
  stat_smooth(span=0.1) +
    coord_cartesian(xlim = c(1,204), ylim = c(0,1)) + theme(legend.position = 'none')

# group average - far well, + indiv traces
ggplot(data = data %>% filter(!is.na(choseFar)) %>% arrange(trial), aes(x=trial, y=1*choseFar)) + 
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.00, color='black') +
  stat_smooth(aes(group=id), se=F, span=1.5, color='gray', method='loess') +
  stat_smooth(span=0.1, se=T) +
  coord_cartesian(xlim = c(1,204), ylim = c(0,1)) + theme(legend.position = 'none')

# group average - far well, + indiv traces, moving avg
ggplot(data = data %>% filter(!is.na(choseFar)) %>% arrange(trial), aes(x=trial, y=1*choseFar)) + 
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.00, color='black') +
  geom_ma(n = 20, ma_fun = ZLEMA, linetype=1, aes(group=id), color='gray') + 
  geom_ma(n = 150, ma_fun = ZLEMA, linetype=1, color='blue') + 
  coord_cartesian(xlim = c(1,204), ylim = c(0,1)) + theme(legend.position = 'none')

data %>% group_by(id) %>% summarize(n = n()) %>% group_by(n) %>% tally()

ggplot(data = data %>% filter(is.na(choseFar) & blocktype != 'devalue') %>% arrange(trial), aes(x=trial, y=1*optimal_choice)) + 
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.00, color='black') +
  geom_ma(n = 25, ma_fun = EMA, color = "red", linetype=1) + 
  coord_cartesian(xlim = c(1,204), ylim = c(0,1)) + theme(legend.position = 'none')


# group average - RT
ggplot(data = data %>% filter(!is.na(rt)) %>% arrange(trial), aes(x=trial, y=rt, color=as.factor(choiceType))) + 
  geom_rect(aes(xmin = 75*(blocknum-1), xmax = 75*blocknum, ymin = -0.5, ymax = Inf, fill = blocknum), alpha = 0.00, color='black') +
  stat_smooth(span = 0.5) +
  coord_cartesian(xlim = c(1,204), ylim = c(400,800)) + theme(legend.position = 'top')


data %>% group_by(id, blocktype) %>% summarize(choseFar = sum(choseFar, na.rm=T), avoidedFar = sum(avoidedFar, na.rm=T), pChoseFar = choseFar / (choseFar + avoidedFar)) %>%
  mutate(blocktype = fct_relevel(blocktype, c('init','switch1','rev2','devalue'))) %>%
  group_by(blocktype) %>% summarize(pChoseFar = mean(pChoseFar, na.rm=T), pChoseFar_sd = sd(pChoseFar, na.rm=T), pChoseFar_se = sd(pChoseFar, na.rm=T)/sqrt(n()), n=n())  
 

# compute % choseFar in final block, plot vs age
habitBeh <- data %>% filter(trial > 140) %>% group_by(id, age.x, age.y) %>% 
  summarize(pHabit = sum(choseFar, na.rm=T) / (sum(choseFar, na.rm=T) + sum(avoidedFar, na.rm=T)))
ggplot(data = habitBeh %>% filter(age.x < 50 & age.x > 18), aes(x=age.x, y=pHabit)) + geom_point() + stat_smooth(method='loess') + coord_cartesian(ylim=c(0,1))
 

ggplot(data=habitBeh, aes(age.x)) + geom_histogram()
