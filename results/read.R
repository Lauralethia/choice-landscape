# ----
# provides 'fix_and_save' reading in raw.tsv and saving out data*.tsv
# sourced by Makefile
#
# NB. works for lab pilot where id encodes age and sex
#     will need to merge lunaid to age sex later.
# (pre-20220412) also handle age in survey
# 20211101WF - init
# 20220412WF - mturk data! have new block devalueAll (all 75) or devalueGood (only good 75)
#              add_blocktype has init switch davalue_all_100 devalue_all_low devalue_good_low
# ----
suppressPackageStartupMessages({library(dplyr)
library(tidyr)})

extract_block_cols <- function(d)
    d %>%
    extract(block,
      into=c('left_pos', 'left_prob',
             'up_pos', 'up_prob',
             'right_pos', 'right_prob'),
       'L(c|f)(\\d+)U(c|f)(\\d+)R(c|f)(\\d+)',
      remove=F) %>%
      # make sure probs are numeric
      mutate_at(vars(matches("(left|right|up)_prob")), as.numeric)

# fix bug on+before 20211029 where step_picked is erroneously step_avoid
# prob for each side is same within a block, and step is the same throughout
# the block identifier string stores that info
# we can pair that with the "picked" (the side) which is reported correctly
fix_prob_picked <- function(d){
   d <- d %>% extract_block_cols()
   nonaidx <- which(d$picked %in% c("left","up","right"))
   picked_col_prob <- paste(sep="_",d$picked, "prob")
   picked_col_pos <- paste(sep="_", d$picked, "pos")
   picked_probs <- sapply(nonaidx,
                          function(i) d[i,picked_col_prob[i]])
   picked_step <- (sapply(nonaidx,
                         function(i) d[i,picked_col_pos[i]]) == "c") %>%
       ifelse(1, 2)
   picked_prob_num <- as.numeric(picked_probs)
   d$picked_prob[nonaidx] <- picked_prob_num
   d$picked_step[nonaidx]    <- picked_step
   return(d)
}

# fix error in issuing urls
# gave /task/id/... instead of /id/task/...
# no id should have contain "habit"
fix_url_order <- function(d){
  bad_url <- grepl('habit', d$id)
  corrected_task <- d[bad_url, 'id']
  corrected_id <- d[bad_url, 'task']
  d[bad_url, 'task'] <- corrected_task
  d[bad_url, 'id'] <-  corrected_id
  return(d)
}

# pilot ids contain initials, age, and sex
# like WWF34M
labpilot_id_age_sex <- function(d) d %>%
     extract(id,
             into=c('initials', 'age', 'sex'),
             '^([A-Za-z]+)(\\d{2})([MmFf_])',
             remove=F) %>%
    # trust survey age if it exists over age that maybe wasn't extracted from id
    mutate(age=as.numeric(ifelse(!is.na(survey_age), survey_age, age)))

# might not want to share that data online
obscure_id <- function(d) d %>% mutate(id=sapply(id,digest::digest))  %>% select(-initials)

# 50 up for some is 20 up for others
# want to have the same for each subject
# so call up left or right e.g. low-first instead of side
unify_picked <- function(d){
 first_info <- d %>%
     group_by(id, ver, timepoint, run) %>%
     filter(trial==1) %>%
     select(left_prob, up_prob, right_prob) %>%
     gather(side, prob, matches('prob')) %>%
     mutate(side=gsub('_prob','',side), prob=paste0('first',prob))
 
 d %>% left_join(rename(first_info, picked=side, picked_unified=prob)) %>%
       left_join(rename(first_info, avoided=side, avoid_unified=prob))
} 

remove_will <- function(d) d %>% filter(!grepl('^WWF|^will|^WFTEST',id))

# nice to have close are far explicit
# but same info is in picked_step
label_distance <- function(d)
   d %>% mutate(pick_dist = cut(picked_step,
                                breaks=c(0,1,Inf),
                                labels=c("close","far")))

# ideally would pick highest prob well
# and when all are equal, pick the closest
add_optimal <- function(d)
  d %>% mutate(optimal_choice =
                   picked_prob > avoided_prob |
                   (picked_prob == avoided_prob & picked_step <= avoided_step))

# 20211102: 3 blocks. init, switch, devalue
# add_blocktype(d) %>% group_by(id,ver,timepoint,run,blocktype) %>% summarise(min(trial), max(trial), n=n())
add_blocktype <- function(d)
    d %>%
     group_by(id, ver, timepoint, run) %>%
        mutate(blocknum = cumsum(lag(block,default=first(block))!=block)+1,
               blocktype = case_when(
                     (left_prob==right_prob) & (up_prob==right_prob) & (up_prob == 100) ~ 'devalue_all_100',
                     (left_prob==right_prob) & (up_prob==right_prob) & (up_prob < 100) ~ 'devalue_all_low',
                     ((left_prob==100) + (right_prob==100) + (up_prob==100)) == 2 ~ 'devalue_good_low', # TODO: hacky. might need to revisit
                     block == first(block) & blocknum<=1 ~ 'init',
                     block != first(block) ~ paste0('switch',blocknum -1),
                     TRUE ~ 'unknown')) %>%
    # survey has own iti? not an actual trial
    filter(block!="")

test_add_blocktype<-function(){
    require(testthat)
    mkblocks <- function(bl)
        data.frame(id=1,ver=1,timepoint=1,run=1, block=bl) %>%
            extract_block_cols %>% add_blocktype


    # reps okay
    has_reps <- mkblocks(c("Lc50Uf100Rc20", "Lc50Uf100Rc20", "Lc20Uf100Rc50", "Lc20Uf100Rc50"))
    expect_equal(has_reps$blocktype, c("init","init","switch1","switch1"))
    expect_equal(has_reps$blocknum, c(1,1,2,2))

    # original scheme
    orig3block <- mkblocks(c("Lc50Uf100Rc20","Lc20Uf100Rc50", "Lc100Uf100Rc100"))
    expect_equal(orig3block$blocktype, c("init","switch1","devalue_all_100"))

    # new 4 block. when all are equal at the end
    block4alldeval <- mkblocks(c("Lc50Uf100Rc20","Lc20Uf100Rc50", "Lc100Uf100Rc100", "Lc75Uf75Rc75"))
    expect_equal(block4alldeval$blocktype, c("init","switch1","devalue_all_100", "devalue_all_low"))

    # new 4 block. good is devalued
    block4alldeval <- mkblocks(c("Lc50Uf100Rc20","Lc20Uf100Rc50", "Lc100Uf100Rc100", "Lc100Uf75Rc100"))
    expect_equal(block4alldeval$blocktype, c("init","switch1","devalue_all_100", "devalue_good_low"))

    # no switch/reversal
    block3norev <- mkblocks(c("Lc50Uf100Rc20", "Lc100Uf100Rc100", "Lc75Uf75Rc75"))
    expect_equal(block3norev$blocktype, c("init","devalue_all_100", "devalue_all_low"))
}

read_taskdata <- function(fpath='raw.tsv'){
    d <- read.csv(fpath, header=T, sep="\t") %>%
        fix_prob_picked %>%
        fix_url_order %>%
        label_distance %>%
        add_optimal %>%
        add_blocktype %>%
        labpilot_id_age_sex %>%
        unify_picked %>%
        remove_will
}
inspect_one <- function(ex_id='WWF',ex_ver='v6'){
    d <- read.csv('raw.tsv', header=T, sep="\t") %>%
        fix_prob_picked %>%
        fix_url_order %>%
        label_distance %>%
        add_optimal %>%
        add_blocktype %>%
        labpilot_id_age_sex %>%
        unify_picked %>%
        filter(grepl(ex_id,id),grepl(ex_ver,ver))
}

# tie it all together. could all this "main"
# two version. one for using locally and another without ids
# sourced and run by Makefile (and pilot_plot.R)
fix_and_save <- function(infile="raw.tsv", outname="data.tsv") {
    read_taskdata(infile) %>%
        write.table(file=outname, sep="\t", row.names=F, quote=F)
    #read_taskdata(infile) %>%
    #    obscure_id %>%
    #    write.table(file="data_id-hidden.tsv",
    #                sep="\t", row.names=F, quote=F)
}

#' READ_TASK_STDIN - used with jq parsing to write file for single run
read_task_stdin <- function() {
   file('stdin') %>% read_taskdata() %>% write.table(sep="\t", row.names=F, quote=F)
}
