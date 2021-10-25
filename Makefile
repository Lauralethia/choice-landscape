.PHONY: .ALWAYS

.ALWAYS:

out/landscape.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --optimizations advanced -c landscape.core
out/card.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --compile landscape.core -A:hostedcards

#mkifdiff from lncdtools
results/data.json: .ALWAYS
	psql `heroku config |sed -n s/DATABASE_URL:.//p` -A -qtc \
		"select json_agg(json_build_object('id',worker_id,'task',task_name,'ver', version,'json', json::json)) from run where finished_at is not null;" | \
		mkifdiff $@;

results/data.tsv: results/data.json
	results/dbjsontotsv.jq < $<  > $@
