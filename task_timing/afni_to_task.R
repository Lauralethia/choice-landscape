#!/usr/bin/env Rscript
suppressPackageStartupMessages(library(dplyr))


prob_blocks  <- list(c(50,20,100),
                c(20,50,100),
                c(100,100,100))
block_rat <- c(1/3, 1/3, 1/3)

# default order has "right" as the "good" choice/side/well/mine/chest
# use rev(choice_order) to swap. (NB. up is never good b/c it feels different)
choice_order <- c("left","up","right") 

glist <- list(c("true","true","false"),
              c("true","false","true"))
nglist <-list(c("true","false","true"))

ISIDUR <- 1000


# {:left {:step 1, :open true, :prob 20}, :up   {:step 1, :open true, :prob 50}, :right{:step 2, :open false, :prob 100}, :iti-dur 1}
build_trial <- function(probs, open, iti,
                        catch=0, steps=c(1,1,1), choices=c("left","up","right")) {

    # ":catch-dur XX" absent on normal trials
    catchdur <- ifelse(catch>0,paste0(':catch-dur ', catch),'')

    # "edn" (similiar to json but for clojure) output
    x <- glue::glue(.open="(",.close=")", "
{:(choices[1]){:step (steps[1]), :open (open[1]), :prob (probs[1])},
 :(choices[2]){:step (steps[2]), :open (open[2]), :prob (probs[2])},
 :(choices[3]){:step (steps[3]), :open (open[3]), :prob (probs[3])},
 :iti-dur (iti*1000), (catchdur)}")
    return(x)
}

# file used
fname <- 'out/500s/v1_102_31234/events.txt'
d <- read.table(text=system(intern=T,glue::glue("./show_times.bash {fname}")))
names(d) <- c("event", "iti")
d$catch <- 0
# fixied isi duration
d$catch[grepl('catch', d$event)] <- ISIDUR


d_tally <- d %>% group_by(event) %>% tally 
n_events <- t(d_tally %>% select(n)) %>% as.list() %>% `names<-`(d_tally$event)
# event    |  n
#----------+---
# good       44
# nogood     23
# ng_catch   11
# g_catch    23

opens <- list(good    =sample(rep(glist, ceiling(n_events$good/2))),
              g_catch =sample(rep(glist, ceiling(n_events$g_catch/2))),
              nogood  =rep(nglist, n_events$nogood),
              ng_catch=rep(nglist, n_events$ng_catch))

# repeat for each
trials_per_block <- ceiling(block_rat*nrow(d))
trial_probs <- mapply(function(prob,n) rep(prob,each=n), prob_blocks, trials_per_block)

res <- c() 
cnts <- sapply(unique(d$event),function(x) 0)
for(i in 1:nrow(d)){
    # update count
    event <- d$event[i]
    j <- cnts[event] + 1 ; cnts[event] <- j
    this_open <- opens[[event]][[j]]
    res[i] <- build_trial(trial_probs[i,],
                          this_open, d$iti[i], d$catch[i], choices=choice_order)
}

# final output
# NB. in EDN, commas are optional/equivlant-to-whitespace
cat("[", paste0(collapse=",\n", res),"]" )
