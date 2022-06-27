#!/usr/bin/env bash
[ $# -eq 0 ] && echo "USAGE: $0 out/500s/v1_102_31234/events.txt" >&2 && exit 1
for fname in "$@"; do
  # needed to get the last iti
  maxtime=$(grep -Po '\d+(?=s)' <<< "$fname" | sed 1q)
  #echo "# fname: $fname maxtime: $maxtime" >&2
  sed '1,3d;$d;s/(\|)//g' "$fname" |
      awk -v mt="$maxtime" '/good|nogood|catch/{e=$2}; !/ 0.00$/{print e, $5} END{print e, mt-$3}'
done
