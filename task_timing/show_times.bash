#!/usr/bin/env bash
[ $# -eq 1 ] && fname="$1" || fname=out/500s/v1_102_31234/events.txt 
sed '1,3d;$d;s/(\|)//g' $fname |
    awk '/good|nogood|catch/{e=$2}; !/ 0.00$/{print e, $5} END{print e, 500-$3}'
