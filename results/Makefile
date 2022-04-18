.PHONY: .ALWAYS all
.ALWAYS:

all: summary.csv

#mkifdiff from lncdtools
raw.json: .ALWAYS
	psql `./dburl` -A -qtc \
		"select json_agg(json_build_object('id',worker_id,'task',task_name,'created_at', created_at, 'finished_at',finished_at, 'ver', version,'timepoint',timepoint, 'run_number',run_number,'json', json::json)) from run where finished_at is not null;" | \
		mkifdiff $@;

raw.tsv: raw.json
	./dbjsontotsv.jq < $<  > $@

survey.tsv: raw.json
	./extra_info.jq < $<  > $@

data.tsv data_id-hidden.tsv: raw.tsv
	Rscript -e "source('./read.R'); fix_and_save()"
summary.csv: data.tsv survey.tsv
	./summary.R
