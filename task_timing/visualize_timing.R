library(ggplot2)
library(dplyr)
library(cowplot)
theme_set(cowplot::theme_cowplot())

files <- c("out/240s/v1_53_31634/events.txt",
           "out/240s/v1_53_20017/events.txt",
           "out/240s/v1_53_19352/events.txt")
files_str <- paste(collapse=" ", files)
cmd <- glue::glue("bash -c 'for f in {files_str}; do ./show_times.bash $f; done'")
timing <- read.table(text=system(intern=T,cmd),
                     sep=" ", col.names=c("type","iti")) %>%
    mutate(catch=grepl("catch", type),
           good=grepl("^g", type),
           trial=1:n())

#ggplot(timing) +
#  aes(x=trial, y=good, size=iti) +
#    geom_point(aes(color=catch)) 
cumsum_reset <- function(x) {
    cnt <- rep(0, length(x))
    for(i in seq(2,length(x))) {
        if(x[i] == x[i-1]) cnt[i] <- cnt[i-1] + 1
    }
    return(cnt)
}

timing_mat <- timing %>%
    mutate(exist=TRUE) %>%
    #tidyr::complete(trial, catch, good, fill=list(exist=FALSE)) %>%
    select(trial, catch, good, exist) %>% 
    tidyr::gather("prop","value", -trial, -exist) %>%
    arrange(trial) %>% group_by(prop) %>%
    mutate(nrep=cumsum_reset(value))


perm <- ggplot(timing_mat) +
    aes(x=trial, y=prop, fill=nrep) +
    geom_raster() +
    scale_fill_continuous(low="lightgray",high="red")
iti <- ggplot(timing) + aes(x=trial,y=1, fill=iti) + geom_raster() + scale_fill_continuous(low="lightgray",high="red")

cowplot::plot_grid(perm,iti, nrow=2)
