library(dplyr)
library(ggplot2)
theme_set(cowplot::theme_cowplot())

d <- read.csv(file="data.tsv", sep="\t")
d_v3 <- d %>% filter(grepl("v3", ver))

p_rt <- ggplot(d_v3) +
    aes(y=rt, x=id, fill=pick_dist) +
    geom_violin() + ggtitle("RT")

d_picks <- d_v3 %>%
    mutate(best_pick=case_when(
                avoided_prob>picked_prob ~ FALSE,
                picked_step >avoided_step ~ FALSE,
                is.na(picked_step) ~ FALSE,
                TRUE ~ TRUE)) %>%
    group_by(id)  %>%
    mutate(good_sum=cumsum(best_pick), bad_sum=cumsum(!best_pick)) %>%
    select(id, trial, matches("(good|bad)_sum")) %>%
    gather(sumtype, val, -id, -trial)

p_bestpick <- ggplot(d_picks) + aes(y=val, x=trial, color=id, group=paste(id,sumtype),linetype=sumtype) + geom_line() + ggtitle('cumulative choice type')


cowplot::plot_grid(p_rt,p_bestpick)

# if using multiple versions:
#  aes(color=ver) +
#    scale_color_manual(values=c("lightgrey","gray", "green"))

d_v3 %>% 
 ggplot() + aes(x=trial, y=paste(picked_prob,picked_unified), color=score) +
 geom_point() + facet_wrap(~id)
