.PHONY: .ALWAYS

.ALWAYS:

out/landscape.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --optimizations advanced -c landscape.core
out/card.js: $(wildcard src/landscape/*cljs src/landscape/*/*.cljs)
	clj -M --main cljs.main --output-to $@ --compile landscape.core -A:hostedcards

#mkifdiff from lncdtools
results/raw.json: .ALWAYS
	psql `results/dburl` -A -qtc \
		"select json_agg(json_build_object('id',worker_id,'task',task_name,'created_at', created_at, 'ver', version,'timepoint',timepoint, 'run_number',run_number,'json', json::json)) from run where finished_at is not null;" | \
		mkifdiff $@;

results/raw.tsv: results/raw.json
	results/dbjsontotsv.jq < $<  > $@

results/data.tsv results/data_id-hidden.tsv: results/raw.tsv
	Rscript -e "setwd('results'); source('./read.R'); fix_and_save()"
