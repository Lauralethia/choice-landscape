out/landscape.js:
	clj -M --main cljs.main --output-to $@ --optimizations advanced -c landscape.core
out/card.js:
	clj -M --main cljs.main --output-to $@ -O advanced --compile landscape.devcards 
