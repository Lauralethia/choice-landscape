#!/usr/bin/env python3
import re
import mne
import numpy as np
import pandas as pd

# plotting
import matplotlib
# need to set the plot type before loading plt
matplotlib.use("TkAgg")
import matplotlib.pyplot as plt

# local extra functions and and a way to reload them
import util
from importlib import reload

## settings
plt.ion()

# print without scientific notation
np.set_printoptions(precision=3)
np.set_printoptions(suppress=True)


## files we're using
bdf = "./data/remotebtn.bdf"
tsv = "./data/remotebtn.tsv"
# bdf='./data/pdint3.bdf'
# tsv='./data/pdtest3.tsv'

# bdf='./data/short_practice_stimtest.bdf'
# tsv='./data/short.tsv'


## read in eeg and get separate stim channel info
eeg = mne.io.read_raw_bdf(bdf)
# eeg.describe() # 247808 (484.0 s) == 512Hz

# events = mne.find_events(eeg, shortest_event=2)
util.add_stim_corrected(eeg)
events = mne.find_events(eeg, stim_channel="StatusCorrected", shortest_event=2)


first_iti_idx = np.where(np.bitwise_and(events[:, 2] >= 10, events[:, 2] <= 15))[0][0]
e_timed = events[first_iti_idx:, [0, 2]].astype("float")
first_event = e_timed[0, 0]
e_timed[:, 0] = (e_timed[:, 0] - first_event) / eeg.info["sfreq"]

t_light = e_timed[e_timed[:, 1] == 1]
t_button = e_timed[np.bitwise_and(e_timed[:, 1] > 1, e_timed[:, 1] < 5)]
t_task = e_timed[np.bitwise_and(e_timed[:, 1] >= 5, e_timed[:, 1] < 256)]


## read in event data
td = pd.read_csv(tsv, sep='\t')
start_time = td["iti_onset"][0]
task_onsets = (
    td.filter(regex="onset")
    .transform(lambda df: (df - start_time) / 1000)
    .melt()
    .sort_values("value")
    .assign(name=lambda d: [re.sub("_onset", "", x) for x in d.variable])
    .reset_index()
)

## Dataframe

task_onsets.iloc["name", task_onsets.name.values == None] = "None"
ttl_df = pd.DataFrame(
    [util.taskttl_event_side(x) for x in t_task[:, 1]],
    columns=["e", "sides", "picked", "score"],
).assign(ttl=t_task[:, 1], onset=t_task[:, 0])

# None as a string so we can color later
ttl_df.loc[ttl_df.e.values == None, "e"] = "None"
factors = np.unique(np.concatenate([ttl_df.e.values, task_onsets.name.values])).tolist()

# iti_ttl used to find/id matching PD events
# iti_task_trunc used to draw lines on plot
iti_ttl = ttl_df[ttl_df.e == "iti"].onset.values
iti_task = task_onsets[task_onsets.name == "iti"].value.values
iti_task_trunc = iti_task[iti_task < iti_ttl[-1] + 0.1]

# difference between task and Stim channel
print(iti_ttl - iti_task_trunc)

# 20220805: are overlapping
# but missing first iti in ttl?
np.stack([np.diff(iti_ttl).T, np.diff(iti_task[1:]).T])[:,0:3]

## plot
plt.close()
plt.plot(t_light, np.repeat(1, len(t_light)), "ro")
# t_button, np.repeat(2,len(t_button)), 'bs',
# task_onsets.value, np.repeat(0,len(task_onsets)), 'k.',
# t_task,   np.repeat(3,len(t_task)), 'g^')

# [0.   ,  0.023,  0.034,  0.011,  0.019,  0.031,  0.011,  0.016, -0.007]
# line for each new trial
for xi in iti_task_trunc:
    plt.axvline(x=xi, color="b")


cvals = [factors.index(f) for f in task_onsets.name.values]
plt.scatter(task_onsets.value, np.repeat(0, len(task_onsets)), c=cvals)

cvals = [factors.index(f) for f in ttl_df.e]
plt.scatter(ttl_df.onset, np.repeat(3, len(ttl_df)), c=cvals)
# plt.scatter(t_task[:,0], np.repeat(2.7,len(t_task)))

cvals = [["blue", "black", "green"][int(x) - 2] for x in t_button[:, 1]]
plt.scatter(t_button[:, 0], np.repeat(2, len(t_button)), c=cvals)

side_factor = ["left", "up", "right"]

for i in range(40):
    plt.annotate(int(t_task[i, 1]), (t_task[i, 0], 3), rotation=30)
for i in range(40):
    plt.annotate(int(t_button[i, 1]), (t_button[i, 0], 2))
for i in range(40):
    plt.annotate(task_onsets.name[i], (task_onsets.value[i], 0), rotation=45)

plt.show()


#### OFFSET
pd_onset = t_light[0, 0]
i = 0
pd_match = []
iti_with_pd = iti_ttl[1:]
for iti in iti_with_pd:
    while i < len(t_light) and len(pd_match) <= len(iti_with_pd):
        if pd_onset > iti:
            pd_match.append(pd_onset)
            break
        pd_onset = t_light[i, 0]
        i = i + 1
iti_to_pd = pd_match - iti_with_pd[0 : len(pd_match)]
np.mean(iti_to_pd) * 1000  # 48.915
np.std(iti_to_pd) * 1000  #  6.009

