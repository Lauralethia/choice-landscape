# Read .json output
quickly abuse lncd scripts to make minimal output for LAF.
There's no database or known source for age, so we just make it up.

See [`LAF.tsv`](LAF.tsv).

|   | file |
|---|---|
|script | `./00_read_LAF.bash`|
|input  | `20240815_LAF_1722548170258.json`|
|output | [`LAF.tsv`](LAF.tsv) |

## Requires
  * `R` w/`tidyr` and `dplyr`. Used by [`../read.R`](../read.R)
  * `jq` Used by [`../dbjsontotsv.jq`](../dbjsontotsv.jq).
  * `bash` to put it together and source `add_info` from [`../read_idv_task.bash`](../read_idv_task.bash)
