library(dplyr)
library(ggplot2)
theme_set(cowplot::theme_cowplot())

d <- read.csv('data.tsv', header=T, sep="\t") %>%
    mutate(pick_dist = cut(picked_step,
                           breaks=c(0,1,Inf),
                           labels=c("close","far")))

# fix error in issuing urls
bad_url <- grepl('habit', d$id)
corrected_task <- d[bad_url, 'id']
corrected_id <- d[bad_url, 'task']
d[bad_url, 'task'] <- corrected_task
d[bad_url, 'id'] <-  corrected_id
write.table(d, file="data_task-id_corrected.tsv",sep="\t", row.names=F, quote=F)

# RT
ggplot(d) + aes(y=rt, x=id,
                fill=pick_dist, color=ver) +
    geom_violin() + ggtitle("RT") +
   scale_color_manual(values=c("gray","black"))
