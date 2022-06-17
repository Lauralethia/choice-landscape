#!/usr/bin/env bash
[ $# -eq 1 ] && fname="$1" || fname=out/500s/v1_102_31234/events.txt 
maxtime=$(grep -Po '\d+(?=s)' <<< $fname)
sed '1,3d;$d;s/(\|)//g' $fname |
    awk -v mt="$maxtime" '/good|nogood|catch/{e=$2}; !/ 0.00$/{print e, $5} END{print e, mt-$3}'
