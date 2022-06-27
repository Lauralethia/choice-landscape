cumsum_reset <- function(x, zero_if_false=TRUE) {
    cnt <- rep(0, length(x))
    for(i in seq(2,length(x))) {
        if(x[i] == x[i-1]) cnt[i] <- cnt[i-1] + 1
        if(zero_if_false & !x[i]) cnt[i] <- 0
    }
    return(cnt)
}
read_timing <- function(event_files){
   cmd <- paste0("./show_times.bash ", paste(event_files, collapse=" "))
   timing <-
      read.table(text=system(intern=T,cmd), sep=" ", col.names=c("type","iti")) %>%
      mutate(catch=grepl("catch", type),
             good=grepl("^g", type),
             trial=1:n())
}

timing_mat <- function(timing) 
   timing %>%
    select(trial, catch, good) %>% 
    tidyr::gather("prop","value", -trial) %>%
    arrange(trial) %>% group_by(prop) %>%
    mutate(nrep=cumsum_reset(value, zero_if_false=all(prop=="catch")))
