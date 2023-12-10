# sourced from habit_plots.R.
# reads output of read.R:fix_and_save(). likely "lab.data.tsv"
# see Makefile
#
# 20231210WF - pull out from habit_plots.R to work on bad merge (FC fixes)
#
# NOTES:
# originally the task had a "far" that was always "good" (prob=highest)
# now "far" means constant-highest
# 
# one version of the task devalued all to a lower prob even the "far"/"good" well: "deval_all_low"
#  we nowe "devalue" by setting all wells to 100% probability: "devalue_all_100"
pacman::p_load(tidyr, dplyr)

# exclude testing runs (IDs with our initials, or 'x')
BAD_IDS <- c("WWF|ACP|^x$")
MIN_TRIALS <- 90

side_label_to_num <- function(side) ifelse(side == 'left', 1, ifelse(side=='up', 2, ifelse(side=='right', 3, NA)))

# "harmonize" blocks to run order: parse known names to 1-4
# devaule always last
# TODO: rewrite with case_when?
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
    testthat::expect_false(format_raw_data('lab.data.tsv')$blocktype %>%
                           unique %>% blocktype_to_num %>% is.na %>% any)
}



true.na <- function(x) !is.na(x) & x
add_choice_cols <- function(data) {
   data %>%
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
}

test_add_choice_cols <- function(){
}

format_raw_data <- function(fname){

  rawdata <- read.csv(fname, sep='\t') %>% 
    filter(!grepl(BAD_IDS, id)) %>%
    mutate(vdate=ifelse(is.na(vdate)|vdate=="", paste0(timepoint, " 00:00:00"), vdate),
           vdate=lubridate::ymd_hms(vdate),
           age = as.numeric(age),
           survey_age = as.numeric(survey_age),
           age=ifelse(is.na(age),survey_age,age))
    #filter(id != 'WWF34M' & id != 'ACP34F' & id != 'AP' & id != 'FC' & ver == '20211104v4-90max_pbar_moredeval')
    #filter(ver == '20211025v3-longertrials')
}

read_raw <- function(fname="lab.data.tsv") {

  rawdata <- format_raw_data(fname)
  
  # have various combinations of blocks
  #   init-switch1-devalue_all_100
  #   init-switch1-devalue_all_100-devalue_all_low
  #
  # cols like:
  #      id vdate               task      ver   timepoint blockseq
  # 11734 2022-11-11 00:00:00 habit_mr  mr_1   20221111 init-switch1-devalue_all_100  
  blockseq_df <-
      rawdata %>% group_by(id,vdate, task, ver,timepoint) %>%
      arrange(trial) %>%
      summarise(blockseq=paste0(unique(blocktype),collapse="-"))
  
  # set fawWell and initHigh side: 1 (left) ,2 (up), or 3 (right)
  farProb <- max(rawdata$up_prob) # expect 100. but might be 95
  first_trial_wellnames <- rawdata %>% filter(trial == 1) %>% 
      mutate(
          # best (was at one time far, likely all same distance now) 
          farWell = ifelse(left_prob == farProb, 1, ifelse(up_prob==farProb, 2, ifelse(right_prob==farProb, 3, NA))),
          # side that was first high (likely of 50/20) 
          initHigh = ifelse(left_prob == 50, 1, ifelse(up_prob==50, 2, 3))) %>%
      select(id, vdate, ver, task, farWell, initHigh)

  # make sure we have enough trials to use
  total_trials <- rawdata %>%
       group_by(id, vdate, task, ver, age=survey_age) %>%
       summarize(ntrials = max(trial)) %>%
       filter(ntrials > MIN_TRIALS)

  nvisits <- nrow(blockseq_df)
  testthat::expect_equal(nvisits, nrow(first_trial_wellnames))
  testthat::expect_equal(nvisits, nrow(total_trials))
   
  perSubj <-
     merge(total_trials, first_trial_wellnames, by=c('id','vdate','ver','task')) %>%
     merge(blockseq_df, by=c("id","vdate","ver","task"),all.x=T)

  # number of visits should match across visit summary stats dataframes
  testthat::expect_equal(nvisits, nrow(perSubj))


  raw_with_visit_summary <-
     merge(rawdata, perSubj,
           by=c('id','vdate', 'ver', 'task','timepoint')) %>%
     add_choice_cols 

  # number of visits should match across visit summary stats dataframes
  testthat::expect_equal(nrow(rawdata), nrow(raw_with_visit_summary))

  return(raw_with_visit_summary)
}

test_read_raw <- function(){
  fname <- "lab.data.tsv"
  raw_orig <- read.table(fname,sep="\t",header=T)
  raw_read <- read_raw(fname)
  # raw shouldn't have added more rows
  testthat::expect_gte(nrow(raw_orig),nrow(raw_read))
  # shouldn't have more than one trial per timepoint+task (habit_mr, habit_eeg)
  n_bad_merge <- raw_read %>% count(id,vdate,timepoint,task,trial) %>% filter(n>1) %>% nrow
  testthat::expect_equal(n_bad_merge,0)
}
