#!/usr/bin/env bash
# windows doesn't use symlinks (by default)
# make git/version control difficult on usb (to transfer to offline windows PC)
# just sync new changes (-u for updated only -- dont change things newer on usb)
rsync --size-only -urvLhi ../choice-landscape/ /mnt/usb/task/choice-landscape/ \
   --exclude .git \
   --exclude results \
   --exclude target \
   --exclude .cljs_node_repl \
   --exclude .cpcache \
   --exclude 'clojure-tools*' \
   --exclude '*.sqlite3' \
    --exclude  .mypy_cache \
    --exclude out/cljs \
   "$@"
