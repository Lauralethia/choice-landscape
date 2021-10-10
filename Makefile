out/landscape.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --optimizations advanced -c landscape.core
out/card.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --compile landscape.core -A:hostedcards
