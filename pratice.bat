Rem run webserver to access task
Rem assume have psiclj.exe from native-image in this directory build 
Rem will need to replace symlinks with files from resources/public/
Rem     out/imgs/ out/audio/ out/style.css
Rem shouldn't need to set password, server is open if running on localhost
set HTTPDBPASS=""
set RUNTOKEN="psiclj_%RANDOM%"
set FIREFOX="C:\Users\localadmin\Downloads\FirefoxPortable\FirefoxPortable.exe"


Rem lots of options for running. using local on 32bit wins + server via java .jar
Rem no sound from filesystem!? and server gives us sqlite3 backup and built in sub-..ses-... json parsing saving
set LINK="file:///C:/Users/localadmin/Desktop/choice-landscape/out/index.html#landscape=mountain&nocaptcha&timing=practice&where=practice"
set LINK="https://labneurocogdevel.github.io/choice-landscape/out/index.html#landscape=mountain&nocaptcha&timing=practice&where=practice"
set LINK="https://lncdwells.herokuapp.com/practice.html"
set LINK="http://127.0.0.1:3001/practice.html"


Rem Win7 matlab task computer is is 32 bit! compiled binary doesn't work!
Rem start %RUNTOKEN%  cmd /c psiclj
start %RUNTOKEN% cmd /c java -cp psiclj.jar clojure.main -m psiclj

Rem wait for java to load up
timeout /t 5

start "" %FIREFOX% %LINK%

