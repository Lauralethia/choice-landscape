library(ggplot2)
library(dplyr)
library(cowplot)
source('funcs.R')
theme_set(cowplot::theme_cowplot())

#files <- c("out/240s/v1_53_31634/events.txt",
#           "out/240s/v1_53_20017/events.txt",
#           "out/240s/v1_53_19352/events.txt")
seeds <- c(6156,23263,1599)
files <- paste0("out/240s/v1_53_",seeds,"/events.txt")
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
    labs(y="",x="", title=glue::glue("seeds: {paste(collapse=', ',seeds)}")) 
iti <- ggplot(timing) + aes(x=trial,y=1, fill=iti) + geom_raster() +
   scale_fill_continuous(low="lightgray",high="blue") +
   labs(y="iti", fill="dur(s)") +
   theme(axis.text.y=element_blank(), axis.ticks.y=element_blank())

cowplot::plot_grid(perm,iti, nrow=2, align="v", rel_heights=c(2,1)) 
