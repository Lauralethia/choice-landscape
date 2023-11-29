Rem test python bridge for ltp and cedrus box hardware
set PYTHON=C:\Program Files\PsychoPy\python.exe
echo ctrl-c to stop each test
echo TESTING CEDRUS BOX
"%PYTHON%" http_ttl.py test_cedrus
echo TESTING LPT
"%PYTHON%" http_ttl.py test_lpt
