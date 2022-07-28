#!/usr/bin/env -S jq -Mfr
["id","task","vdate","timepoint", "run", "ver","avatar",
 "trial","block","score",
 "rt","n_keys", "first_key_rt", "first_key",
 "picked","picked_prob","picked_step",
 "avoided","avoided_prob","avoided_step",
 "iti_onset","chose_onset","waiting_onset","feedback_onset","timeout_onset","trial_choices", "survey_age"],
(.[]|
   .id as $id|
   .timepoint as $tp|
   .run_number as $rn |
   .task as $task|
   .ver as $ver|
   .created_at as $vdate|
   .json.avatar as $av|
   .json."start-time".browser as $btime|
   .json."start-time".animation as $atime|
   .json.survey as $survey|
   .json.events[] |
   ."chose-time" as $ct |
   [$id, $task, $vdate, $tp, $rn, $ver, $av,
    .trial, .blockstr, .score,
    if(."waiting-time") then (."waiting-time" - $ct)|round else null end,
    if(."all-keys") then ."all-keys"|length else null end,
    if(".all-keys" and $btime and $atime) then
       (."all-keys"[0] as $t | if($t) then ($t.time  - $btime + $atime - $ct)|round else null end)
       else null end,
    if(".all-keys") then ."all-keys"[0].key else null end,
    ."picked", ."picked-prob", ."picked-step", ."avoided", ."avoided-prob", ."avoided-step",
    ."iti-time", $ct, ."waiting-time", ."feedback-time", ."timeout-time",
    ."trial-choices",
    if($survey|type=="array") then "NA" else $survey.age end] )|
   @tsv
