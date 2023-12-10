read_raw <- function(fname="lab.data.tsv") {

  # exclude testing runs (IDs with our initials, or 'x')
  BAD_IDS <- c("WWF|ACP|^x$")
  MIN_TRIALS <- 90

  rawdata <- read.csv(fname, sep='\t') %>% 
    filter(!grepl(BAD_IDS, id)) %>%
    mutate(vdate=ifelse(is.na(vdate)|vdate=="", paste0(timepoint, " 00:00:00"), vdate),
           vdate=lubridate::ymd_hms(vdate),
           age = as.numeric(age),
           survey_age = as.numeric(survey_age),
           age=ifelse(is.na(age),survey_age,age))
    #filter(id != 'WWF34M' & id != 'ACP34F' & id != 'AP' & id != 'FC' & ver == '20211104v4-90max_pbar_moredeval')
    #filter(ver == '20211025v3-longertrials')
  
  # have various combinations of blocks
  #   init-switch1-devalue_all_100
  #   init-switch1-devalue_all_100-devalue_all_low
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
   
   
  perSubj <- merge(total_trials, first_trial_wellnames, by=c('id','vdate','ver','task'))

  rawdata %>%
      merge(perSubj, by=c('id','vdate', 'ver', 'task')) %>%
      left_join(blockseq_df, by=c("id","vdate","ver","task","timepoint")) %>%
  add_choice_cols 
}
