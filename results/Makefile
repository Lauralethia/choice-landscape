.PHONY: .ALWAYS all
.ALWAYS:

all: summary.csv data_all.tsv

#mkifdiff from lncdtools
raw.json: .ALWAYS
	psql `./dburl` -A -qtc \
		"select json_agg(json_build_object('id',worker_id,'task',task_name,'created_at', created_at, 'finished_at',finished_at, 'ver', version,'timepoint',timepoint, 'run_number',run_number,'json', json::json)) from run where finished_at is not null;" | \
		mkifdiff $@;

raw.tsv: raw.json
	./dbjsontotsv.jq < $<  > $@

survey.tsv: raw.json
	./extra_info.jq < $<  > $@

bea_res_concat.tsv: $(wildcard /Volumes/L/bea_res/Data/Tasks/Habit/MR/1*_2*/*tsv)
	cat_skiphead $? > $@

data_id-hidden.tsv: data.tsv
data.tsv: raw.tsv
	Rscript -e "source('./read.R'); fix_and_save()"

summary.csv: data.tsv survey.tsv
	./summary.R

data_all.tsv: data.tsv bea_res_concat.tsv
	cat_skiphead $? > $@
