Rem run webserver to access task
Rem assume have psiclj.exe from native-image in this directory build 
Rem will need to replace symlinks with files from resources/public/
Rem     out/imgs/ out/audio/ out/style.css
Rem shouldn't need to set password, server is open if running on localhost
set HTTPDBPASS=""
set RUNTOKEN="psiclj_%RANDOM%"
set FIREFOX="C:\Users\Luna\FirefoxPortable\FirefoxPortable.exe"

start %RUNTOKEN%  cmd /c psiclj
Rem launching into mr page. will set #where=mri anchor
start "" "%FIREFOX%" http://127.0.0.1:3001/mr.html
