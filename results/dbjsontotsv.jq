#!/usr/bin/env -S jq -Mfr
["id","task","vdate","timepoint", "run", "ver","avatar",
 "trial","block","score",
 "rt",
 "picked","picked_prob","picked_step",
 "avoided","avoided_prob","avoided_step",
 "iti_onset","chose_onset","waiting_onset","feedback_onset","trial_choices", "survey_age"],
(.[]|
   .id as $id|
   .timepoint as $tp|
   .run_number as $rn |
   .task as $task|
   .ver as $ver|
   .created_at as $vdate|
   .json.avatar as $av|
   .json.survey as $survey|
   .json.events[] |
   [$id, $task, $vdate, $tp, $rn, $ver, $av,
    .trial, .blockstr, .score,
    if(."waiting-time") then (."waiting-time" - ."chose-time")|round else null end,
    ."picked", ."picked-prob", ."picked-step", ."avoided", ."avoided-prob", ."avoided-step",
    ."iti-time", ."chose-time", ."waiting-time", ."feedback-time", 
    ."trial-choices",
    if($survey|type=="array") then "NA" else $survey.age end] )|
   @tsv
