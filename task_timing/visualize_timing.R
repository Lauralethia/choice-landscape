library(ggplot2)
library(dplyr)
library(cowplot)
source('funcs.R')
theme_set(theme_cowplot())

#files <- c("out/240s/v1_53_31634/events.txt",
#           "out/240s/v1_53_20017/events.txt",
#           "out/240s/v1_53_19352/events.txt")
# seeds <- c(6156,23263,1599) # 20220623 - v025 seeds. min iti was 0.25
#seeds <- c(25647,10141,12240); GEN_VER<-"v1.5"; TIME<-240  # 20220623
seeds <- c(10987, 32226, 24271);GEN_VER<-"v1.5"; TIME<-280  # 20220627
files <- paste0("out/", TIME, "s/",GEN_VER,"_53_",seeds,"/events.txt")
timing <- read_timing(files) #%>% mutate(iti=as.numeric(iti))
tmat <- timing_mat(timing)

inc_idx <- with(tmat, prop=="catch" & value)
tmat$nrep[inc_idx] <- tmat$nrep[inc_idx] + 1
# tell good from bad by making bad negative
neg_idx <- with(tmat, prop=="good" & !value)
tmat$nrep[neg_idx] <- tmat$nrep[inc_idx] * -1

perm <- ggplot(tmat) +
    aes(x=trial, y=prop, fill=nrep) +
    geom_raster() +
    scale_fill_gradient2(low="black",mid="white",high="red",
                         breaks=c(min(tmat$nrep),0,max(tmat$nrep))) +
    labs(y="",x="", title=glue::glue("miniti {GEN_VER}; block dur {TIME}; seeds: {paste(collapse=', ',seeds)}")) +
    theme(legend.position = "top") # c(-0, .8))
iti <- ggplot(timing) + aes(x=trial,y=1, fill=iti) + geom_raster() +
   scale_fill_continuous(low="lightgray",high="blue") +
   labs(y="iti", fill="dur(s)") +
   theme(axis.text.y=element_blank(), axis.ticks.y=element_blank(),
         legend.position = "bottom")# c(-0, .9))

task_dist <- timing %>% select(catch,good) %>% tidyr::gather() %>%
   ggplot() + aes(fill=value, x=key) + geom_histogram(stat="count", position="dodge") +
   theme(legend.position="none")


iti_dist <- ggplot(timing) + aes(x=iti) + geom_histogram(binwidth=.5)

plots_dist <- plot_grid(task_dist,iti_dist, nrow=2, align="v") 
plots_rep <- plot_grid(perm,iti, nrow=2, rel_heights=c(2,1), align="v") 

plot_grid(plots_rep, plots_dist, ncol=2, rel_widths=c(3,1))
