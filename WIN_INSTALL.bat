Rem
Rem Install extra bits after psychopy and git clone
Rem

Rem location of psychopy, reusing it's python interpreter
set PYTHON=C:\Program Files\PsychoPy\python.exe

Rem install 'tornado' and 'pynput'
"%PYTHON%" -m pip install -r requirements.txt
echo you might also need python packages: pyxid2 for Cedrus OR usb_1208FS and RTBox for sEEG

Rem get psiclj for saving to sqlite database
powershell -Command "Invoke-WebRequest https://github.com/LabNeuroCogDevel/psiclj/releases/download/v0.2.3/psiclj.exe -OutFile psiclj.exe"

Rem out is symlinked but that doesn't work on windows
echo remove out/audio/ out/images out/style.css
echo xcopy resources/public/* out/
