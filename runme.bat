Rem run webserver to access task
Rem assume have psiclj.exe from native-image in this directory build 
Rem will need to replace symlinks with files from resources/public/
Rem     out/imgs/ out/audio/ out/style.css
Rem shouldn't need to set password, server is open if running on localhost
set HTTPDBPASS=""
set RUNTOKEN="psiclj_%RANDOM%"

start %RUNTOKEN%  cmd /c psiclj
Rem launching into mr page. will set #where=mri anchor
Rem TODO: check if 0.0.0.0 or 127.0.0.1 is better
start "" http://0.0.0.0:3001/mr.html
