.PHONY: .ALWAYS figwheel all test

.ALWAYS:

all: out/landscape.js

# get dependencies for figwheel. maybe this is an issue with .dir-locals.el not loading correctly?
# should only be needed once. also see (hack-dir-local-variables-non-file-buffer)
# if hang (do (require 'figwheel.main) (figwheel.main/start :dev))
# check dev compile
# if this fails to compile, cannot connect to server
figwheel:
	clojure -A:fig -m figwheel.main --build dev # --repl

out/landscape.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --optimizations advanced -c landscape.core
out/card.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --compile landscape.core -A:hostedcards

test:
	clojure -A:test -M -m kaocha.runner

results/summary.csv: .ALWAYS
	cd results && make
