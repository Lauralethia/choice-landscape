#!/usr/bin/env Rscript
suppressPackageStartupMessages(library(dplyr))


PROB_BLOCKS  <- list(c(50,20,100),
                c(20,50,100),
                c(100,100,100))
BLOCK_RAT <- c(1/3, 1/3, 1/3)

# default order has "right" as the "good" choice/side/well/mine/chest
# use rev(choice_order) to swap. (NB. up is never good b/c it feels different)
CHOICE_ORDER <- c("left","up","right")

ISIDUR <- 1000

FIRSTITI <- 3 # in seconds

# {:left {:step 1, :open true, :prob 20}, :up   {:step 1, :open true, :prob 50}, :right{:step 2, :open false, :prob 100}, :iti-dur 1}
build_trial <- function(probs, open, iti,
                        catch=0, steps=c(1,1,1), choices=c("left","up","right")) {

    # ":catch-dur XX" absent on normal trials
    catchdur <- ifelse(catch>0,paste0(':catch-dur ', catch),'')

    probs <- paste(":prob", probs)
    steps <- paste(":step", steps)
    # dont care about probs and steps if only doing good/nogood
    # will set timing later
    if('nogood' %in% choices) {
       probs <- c("","","")
       steps <- c("","","")
    }
    

    # "edn" (similiar to json but for clojure) output
    x <- glue::glue(.open="(",.close=")", "
{:(choices[1]){:open (open[1]), (steps[1]), (probs[1])},
 :(choices[2]){:open (open[2]), (steps[2]), (probs[2])},
 :(choices[3]){:open (open[3]), (steps[3]), (probs[3])},
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
gen_seq <- function(d){
  # d is a dataframe representing events. should have an 'event' column w/event name
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

  # see CHOICE_ORDER <- c("left","up","right")
  # default good is right, rev(CHOICE_ORDER) will make left good
  # 20220919 BUG! there is no false true true
  glist <- list(c("false","true","true"), #prev incorrectly: list(c("true","false","true"))
                c("true","false","true"))
  nglist <-list(c("true","true","false"))

  opens <- list(good    =sample(rep(glist, ceiling(n_events$good/2))),
                g_catch =sample(rep(glist, ceiling(n_events$g_catch/2)%||%0)),
                nogood  =rep(nglist, n_events$nogood),
                ng_catch=rep(nglist, n_events$ng_catch%||%0))
}
read_files_with_iti <- function(files, firstiti){
  # read in each file, and combine. could shuffle but not done here
  d <- lapply(files, read_events) %>% bind_rows
  # iti is first event of trial for task, but modeled as last in 3dDeconvolve
  d <- d %>% mutate(iti=c(FIRSTITI, iti[1:(n()-1)]))
}

gen_probs <- function(n, block_rat=BLOCK_RAT, probs=PROB_BLOCKS){
  trials_per_block <- ceiling(block_rat*n)
  trial_probs <- mapply(function(prob,n) rep(prob,each=n), probs, trials_per_block)
}
gen_edn <- function(files, leftgood=FALSE, goodnogood_names=FALSE){
  d <- read_files_with_iti(files, FIRSTITI)
  opens <- gen_seq(d)

  # order is: left, up, right. unless reversed. up never "good"
  choice_order <- CHOICE_ORDER
  if(leftgood) choice_order <- rev(CHOICE_ORDER)
  if(goodnogood_names) choice_order <- c("good","up","nogood")

  # repeat for each
  trial_probs <- gen_probs(nrow(d), BLOCK_RAT, PROB_BLOCKS)

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
        cat("use --goodnogood to use 'good' and 'nogood' for names\n")
        quit(status=1,save="no")
    }
    leftgood <- grepl("--left",argv)
    goodnogood_names <- grepl("--goodnogood",argv)
    #print(argv)
    #print(leftgood)

    # remove any input args that aren't files
    argv <- argv[!(leftgood|goodnogood_names)]
    #print(argv)

    gen_edn(argv, leftgood=any(leftgood), goodnogood_names=any(goodnogood_names))
}

seq_test <- function(){
    files <- c("/Volumes/Hera/Projects/Habit/task/task_timing/out/280s/v1.5_53_10987/events.txt")
    d <- read_files_with_iti(files, FIRSTITI)
    o <- gen_seq(d)
    l <- rle(sort(sapply(c(o$good, o$nogood),paste,collapse="_")))$lengths

    # have all 3 combos in equal counts
    testthat::expect_true(length(l)==3)
    testthat::expect_true(all(l==12))
    
    # default order is left,up,right. and right is "good"
    # good: (x,x,true)    # two ways
    # bad: is (t,t,false) # only one way

    # good or bad is based on last of 3 values ("is well open" true for good, false for bad
    testthat::expect_true(all("true"==sapply(o$good,`[`,3)))
    testthat::expect_true(all("false"==sapply(o$nogood,`[`,3)))
    # 2 ways to do good. 1 way to do bad
    testthat::expect_equal(length(unique(o$good)),2)
    testthat::expect_equal(length(unique(o$nogood)),1)
    # bad has to be t,t,f
    testthat::expect_equal(unlist(unique(o$nogood)),c("true","true","false"))
}
gen_prob_test <- function(){
  trial_probs <- gen_probs(54*3, BLOCK_RAT, PROB_BLOCKS)
  x <- rle(apply(trial_probs,1, paste,collapse="_"))
  testthat::expect_equal(x$values,  c("50_20_100","20_50_100","100_100_100"))
  testthat::expect_true(all(x$lenghts==54))
}

build_trial_test <- function(){
   res <- build_trial(c(10,20,30), c("true","false","justfortesting"), iti=1000, catch=0, choices=c("left","up","right"))
   testthat::expect_true(grepl(res,pattern=':left.:step 1, :open true, :prob 10'))
   testthat::expect_true(grepl(res,pattern=':up.:step 1, :open false, :prob 20'))
   testthat::expect_true(grepl(res,pattern=':right.:step 1, :open justfortesting, :prob 30'))
}

demo_gen_edn <- function(){
 files <- sprintf(sprintf("out/280s/v1.5_53_%s/events.txt", c("10987", 32226, 24271)))
 x <- gen_edn(files, leftgood=TRUE)

 files <- sprintf(sprintf("out/185s/v1.5-nocatch_34_%s/events.txt", c("22353",7403,28744,23677,31668)))
 x <- gen_edn(files, leftgood=TRUE)
}
