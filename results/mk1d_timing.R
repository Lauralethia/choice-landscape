source('read.R');
wf <- inspect_one('WFTEST2','*');
wf %>% ungroup %>% mutate(f_dur=lead(iti_onset)-feedback_onset,w_dur=feedback_onset-waiting_onset) %>% select(matches('dur')) %>% summary()
#     f_dur           w_dur      
# Min.   :239.6   Min.   :214.7  
# 1st Qu.:262.5   1st Qu.:230.8  
# Median :268.4   Median :236.8  
# Mean   :267.9   Mean   :241.4  
# 3rd Qu.:271.1   3rd Qu.:258.5  
# Max.   :287.9   Max.   :263.1  
# NA's   :1     
 
offset <- wf$iti_onset[1]
timing <- wf %>% ungroup %>%
   mutate(block=1,
          across(matches('onset'),function(x) (x - offset)/1000),
          f_dur=lead(iti_onset)-feedback_onset,
          w_dur=feedback_onset-waiting_onset,
          rt_dur=waiting_onset-chose_onset,
          # NB. timeout trials will not go into this
          # convient for coding and probably the correct analysis
          isgood=grepl('first100', picked_unified)|grepl('first100', avoid_unified),
          noresp=is.na(rt))

blk <-  timing %>% group_by(blocktype) %>%
   summarise(start=first(iti_onset),
             end=max(last(feedback_onset), last(timeout_onset),na.rm=T)) %>%
   arrange(start)
 
# !isgood & !noresp
timing %>% filter(isgood) %T>%
  LNCDR::save1D('chose_onset',   dur='rt_dur')  %T>%
  LNCDR::save1D('waiting_onset', dur='w_dur') %T>%
  LNCDR::save1D('feedback_onset',dur='f_dur') %>%
  invisible

