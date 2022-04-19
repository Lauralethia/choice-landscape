Rem run webserver to access task
Rem assume have psiclj.exe from native-image in this directory build 
Rem will need to replace symlinks with files from resources/public/
Rem     out/imgs/ out/audio/ out/style.css
Rem shouldn't need to set password, server is open if running on localhost
set HTTPDBPASS=""
set RUNTOKEN="psiclj_%RANDOM%"

Rem consider setting task name and getting eg "#where=mri" from db (permutation:anchors)
start %RUNTOKEN%  cmd /c psiclj
start "" http://0.0.0.0:3001
