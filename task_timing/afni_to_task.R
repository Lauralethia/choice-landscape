#!/usr/bin/env Rscript
suppressPackageStartupMessages(library(dplyr))


PROB_BLOCKS  <- list(c(50,20,100),
                c(20,50,100),
                c(100,100,100))
BLOCK_RAT <- c(1/3, 1/3, 1/3)

# default order has "right" as the "good" choice/side/well/mine/chest
# use rev(choice_order) to swap. (NB. up is never good b/c it feels different)
CHOICE_ORDER <- c("left","up","right")

GLIST <- list(c("true","true","false"),
              c("true","false","true"))
NGLIST <-list(c("true","false","true"))
ISIDUR <- 1000

FIRSTITI <- 3 # in seconds

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

read_events <- function(fname='out/500s/v1_102_31234/events.txt'){
  d <- read.table(text=system(intern=T,glue::glue("./show_times.bash {fname}")))
  names(d) <- c("event", "iti")
  d$catch <- 0
  # fixied isi duration
  d$catch[grepl('catch', d$event)] <- ISIDUR
  # was worried we'd be chopping of a variable last iti
  # but it's always 1.5
  # lapply(files, function(f) read_events(f) %>% tail(n=1)) 
  return(d)
}

"%||%" <- function(x, y) if(is.null(x)||length(x)==0L) y else x
gen_edn <- function(files, leftgood=FALSE){
  # read in each file, and combine. could shuffle but not done here
  d <- lapply(files, read_events) %>% bind_rows

  # iti is first event of trial for task, but modeled as last in 3dDeconvolve
  d <- d %>% mutate(iti=c(FIRSTITI, iti[1:(n()-1)]))

  d_tally <- d %>% group_by(event) %>% tally
  n_events <- t(d_tally %>% select(n)) %>% as.list() %>% `names<-`(d_tally$event)
  # event    |  n
  #----------+---
  # good       44
  # nogood     23
  # ng_catch   11
  # g_catch    23

  ### no catch version ###
  # event   n
  # good   115
  # nogood  55

  opens <- list(good    =sample(rep(GLIST, ceiling(n_events$good/2))),
                g_catch =sample(rep(GLIST, ceiling(n_events$g_catch/2)%||%0)),
                nogood  =rep(NGLIST, n_events$nogood),
                ng_catch=rep(NGLIST, n_events$ng_catch%||%0))

  # order is: left, up, right. unless reversed. up never "good"
  choice_order <- CHOICE_ORDER
  if(leftgood) choice_order <- rev(CHOICE_ORDER)

  # repeat for each
  trials_per_block <- ceiling(BLOCK_RAT*nrow(d))
  trial_probs <- mapply(function(prob,n) rep(prob,each=n), PROB_BLOCKS, trials_per_block)

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
}

if (sys.nframe() == 0) {
    argv <- commandArgs(trailingOnly = TRUE)
    if(length(args)==0L) {
        cat("want input files to be combined after ./show_times.bash and turned into edn string for clojure\n like out/240s/v1_53_17/events.txt\n")
        cat("use --left to set left as the first good well\n")
        quit(status=1,save="no")
    }
    leftgood_i <-grep("--left",argv)
    leftgood <- length(leftgood_i)>=1L
    #print(argv)
    #print(leftgood)
    if(leftgood) argv <- argv[-leftgood_i]
    #print(argv)

    gen_edn(argv, leftgood=leftgood)
}

quicktests <- function(){
 files <- sprintf(sprintf("out/280s/v1.5_53_%s/events.txt", c("10987", 32226, 24271)))
 x <- gen_edn(files, leftgood=TRUE)

 files <- sprintf(sprintf("out/185s/v1.5-nocatch_34_%s/events.txt", c("22353",7403,28744,23677,31668)))
 x <- gen_edn(files, leftgood=TRUE)
}
