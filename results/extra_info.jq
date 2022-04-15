#!/usr/bin/env -S jq -Mfr
["id","task","perm", "vdate","endtime",
 "timepoint", "run", "ver","avatar",
 "age","understand","fun","feedback"],
(.[]|
   .id as $id| .timepoint as $tp| .run_number as $rn | .task as $task| .ver as $ver|
   .created_at as $vdate_start| .finished_at as $vdate_end|
   .json.avatar as $av|
   .json.survey as $survey|
   .json.url.anchor as $anchor|
   [$id, $task, $anchor, $vdate_start, $vdate_end,
    $tp, $rn, $ver, $av,
    if($survey|type=="array") then
     [$survey[0].age, $survey[0].understand, $survey[0].fun, $survey[0].feedback] else
     [$survey.age, $survey.understand, $survey.fun, $survey.feedback]
    end
   ]|flatten )|
   @tsv
