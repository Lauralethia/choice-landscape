source('read.R');
wf <- inspect_one("WFTEST2", "*");
wf %>%
   ungroup %>%
   mutate(f_dur=lead(iti_onset)-feedback_onset,
          w_dur=feedback_onset-waiting_onset) %>%
   select(matches("dur")) %>%
   summary()
#     f_dur           w_dur
# Min.   :239.6   Min.   :214.7
# 1st Qu.:262.5   1st Qu.:230.8
# Median :268.4   Median :236.8
# Mean   :267.9   Mean   :241.4
# 3rd Qu.:271.1   3rd Qu.:258.5
# Max.   :287.9   Max.   :263.1
# NA's   :1
 
toffset <- wf$iti_onset[1]
timing <- wf %>%
   mutate(block=1,
          across(matches("onset"), function(x) (x - toffset)/1000),
          f_dur=lead(iti_onset)-feedback_onset,
          w_dur=feedback_onset-waiting_onset,
          rt_dur=waiting_onset-chose_onset,
          # NB. timeout trials will not go into this
          # convient for coding and probably the correct analysis
          isgood=grepl("first100", picked_unified)|
                 grepl("first100", avoid_unified),
          noresp=is.na(rt))

blk <-  timing %>%
   group_by(blocktype) %>%
   summarise(start=first(chose_onset),
             end=max(last(feedback_onset), last(timeout_onset),na.rm=T)) %>%
   arrange(start)

save_events <- function(timing, outprefix="1d/wftest/") {
   fn <- function(evname) paste0(outprefix, evname, ".1D")
   timing  %T>%
      LNCDR::save1D("chose_onset",    fname=fn("chose"))  %T>% # dur="rt_dur", 
      LNCDR::save1D("waiting_onset",  fname=fn("walk")) %T>%   #  dur="w_dur",  
      LNCDR::save1D("feedback_onset", fname=fn("fbk")) %>%     #  dur="f_dur",  
      invisible
}

timing %>% filter(isgood) %>% save_events("1d/wftest/g_")
timing %>% filter(!isgood, !noresp) %>% save_events("1d/wftest/ng_")
# run deconvolve nodata and extract Std using lncdtools 3dDeconLogGLTs
# from lncdtools
# see ../task_timing/01_collect.R for viewing
system("cd 1d/wftest &&
  3dDeconvolve                                                  \\
    -nodata 410 1.300                                           \\
    -polort 1                                                   \\
    -num_stimts 6                                               \\
    -stim_times 1 g_chose.1D GAM                                \\
    -stim_label 1 good                                          \\
    -stim_times 2 g_fbk.1D GAM                                  \\
    -stim_label 2 g_fbk                                         \\
    -stim_times 3 g_walk.1D GAM                                 \\
    -stim_label 3 g_walk                                        \\
    -stim_times 4 ng_chose.1D GAM                               \\
    -stim_label 4 nogood                                        \\
    -stim_times 5 ng_fbk.1D GAM                                 \\
    -stim_label 5 ng_fbk                                        \\
    -stim_times 6 ng_walk.1D GAM                                \\
    -stim_label 6 ng_walk                                       \\
    -num_glt 18                                                 \\
    -gltsym 'SYM: good -g_fbk' -glt_label 1 good-g_fbk          \\
    -gltsym 'SYM: good -g_walk' -glt_label 2 good-g_walk        \\
    -gltsym 'SYM: good -nogood' -glt_label 3 good-nogood        \\
    -gltsym 'SYM: good -ng_fbk' -glt_label 4 good-ng_fbk        \\
    -gltsym 'SYM: good -ng_walk' -glt_label 5 good-ng_walk      \\
    -gltsym 'SYM: g_fbk -g_walk' -glt_label 6 g_fbk-g_walk      \\
    -gltsym 'SYM: g_fbk -nogood' -glt_label 7 g_fbk-nogood      \\
    -gltsym 'SYM: g_fbk -ng_fbk' -glt_label 8 g_fbk-ng_fbk      \\
    -gltsym 'SYM: g_fbk -ng_walk' -glt_label 9 g_fbk-ng_walk    \\
    -gltsym 'SYM: g_walk -nogood' -glt_label 10 g_walk-nogood   \\
    -gltsym 'SYM: g_walk -ng_fbk' -glt_label 11 g_walk-ng_fbk   \\
    -gltsym 'SYM: g_walk -ng_walk' -glt_label 12 g_walk-ng_walk \\
    -gltsym 'SYM: nogood -ng_fbk' -glt_label 13 nogood-ng_fbk   \\
    -gltsym 'SYM: nogood -ng_walk' -glt_label 14 nogood-ng_walk \\
    -gltsym 'SYM: ng_fbk -ng_walk' -glt_label 15 ng_fbk-ng_walk \\
    -gltsym 'SYM: good +nogood' -glt_label 16 choice            \\
    -gltsym 'SYM: g_fbk +ng_fbk' -glt_label 17 fbk              \\
    -gltsym 'SYM: good +nogood -g_fbk -ng_fbk'                  \\
    -glt_label 18 choice-fbk -x1D X.stimes.xmat.1D > decon.log &&
    3dDeconLogGLTs v1.5-410-102-wf < decon.log > std_dev_tests.tsv
    ")
