#!/usr/bin/env -S jq -Mfr
["id","task","ver","avatar",
 "trial","block","score",
 "rt",
 "picked","picked_prob","picked_step",
 "avoided","avoided_prob","avoided_step",
 "iti_onset","chose_onset","waiting_onset","feedback_onset"],
(.[]|
   .id as $id|
   .task as $task|
   .ver as $ver|
   .json.avatar as $av|
   .json.events[] |
   [$id, $task, $ver, $av,
    .trial, .blockstr, .score,
    if(."waiting-time") then (."waiting-time" - ."chose-time")|round else null end,
    ."picked", ."picked-prob", ."picked-step", ."avoided", ."avoid-prob", ."avoid-step",
    ."iti-time", ."chose-time", ."waiting-time", ."feedback-time" ] )|
   @tsv