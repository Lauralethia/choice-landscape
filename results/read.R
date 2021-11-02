# ----
# provides 'fix_and_save' reading in raw.tsv and saving out data*.tsv
# sourced by Makefile
#
# NB. works for lab pilot where id encodes age and sex
#     will need to merge lunaid to age sex later.
#
# 20211101WF - init
# ----
library(dplyr)
library(tidyr)

# fix bug on+before 20211029 where step_picked is erroneously step_avoid
# prob for each side is same within a block, and step is the same throughout
# the block identifier string stores that info
# we can pair that with the "picked" (the side) which is reported correctly
fix_prob_picked <- function(d){
   d <- d %>% extract(block,
      into=c('left_pos', 'left_prob',
             'up_pos', 'up_prob',
             'right_pos', 'right_prob'),
       'L(c|f)(\\d+)U(c|f)(\\d+)R(c|f)(\\d+)',
      remove=F) 
   nonaidx <- which(d$picked %in% c("left","up","right"))
   picked_col_prob <- paste(sep="_",d$picked, "prob")
   picked_col_pos <- paste(sep="_", d$picked, "pos")
   picked_probs <- sapply(nonaidx,
                          function(i) d[i,picked_col_prob[i]])
   picked_step <- (sapply(nonaidx,
                         function(i) d[i,picked_col_pos[i]]) == "c") %>%
       ifelse(1, 2)
   d$prob_picked[nonaidx] <- as.numeric(picked_probs)
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
             '^([A-Za-z]+)(\\d{2})([MmFf])',
             remove=F)

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

label_distance <- function(d)
   d %>% mutate(pick_dist = cut(picked_step,
                                breaks=c(0,1,Inf),
                                labels=c("close","far")))

read_taskdata <- function(fpath='data.tsv'){
    d <- read.csv(fpath, header=T, sep="\t") %>%
        fix_prob_picked %>%
        fix_url_order %>%
        label_distance %>%
        unify_picked
}

# tie it all together. could all this "main"
# two version. one for using locally and another without ids
# sourced and run by Makefile (and pilot_plot.R)
fix_and_save <- function(fname="raw.tsv") {
    read_taskdata(fname) %>%
        write.table(file="data.tsv",
                    sep="\t", row.names=F, quote=F)

    read_taskdata(fname) %>%
        labpilot_id_age_sex %>%
        obscure_id %>%
        write.table(file="data_id-hidden.tsv",
                    sep="\t", row.names=F, quote=F)
}
