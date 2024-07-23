## Usage
use `./habit_seeg` to launch. bundles 3 steps that can be run individually to debug

 - `python ./http_ttl.py seeg` connect task to button box and DAQ. and DAQ to buttonbox
   - `python ./http_ttl.py test_rtbox` to check rt box is working
 - `java -cp psiclj.jar clojure.main -m psiclj &` run the task in a small http server for sqlite writting. Also avoid CORS issues with TTL http requets
 - `firefox http://127.0.0.1:3001/seeg.html &` the task in a browser


## Install
see [`rtbox_seeg/readme.md`](rtbox_seeg/readme.md)
