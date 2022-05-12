2022-05-11 WF
 * copy current task. but might want to use online version anyway
 * DAQ box python tools (uldaq), RTBox (pyserial pynput psychopy)

    ```
    sudo apt install python3-pip swig libusb-1.0-0-dev
    # swig for pocketsphinx for psychopy
    # libusb for uldaq
    python3 -m pip install uldaq pip pynput pyserial psychopy wxpython ipython --user
    ```
 * RTBox
git clone https://github.com/xiangruili/RTBox_py
  ```
cd RTBox/python/
python3 RTBoxAsKeypas.py # exits immedietly 
# use own version
``` 

use lots of water for suction cup


## in ipython
```
import RTBoxAsKeypad
box = RTBoxAsKeypad.RTBox.RTBox()
#box.clockRatio()
box.info()
box.enable("light")
box.threshold(4)
kp = RTBoxAsKeypad.RTBoxAsKeypad() # autostarts
box.disable('light');box.enable('light');
```

ipython3 ~/luna_habit/RTBox_py/RTBox_light_demo.py 

----
ipython3 ~/luna_habit/RTBox_py/RTBox_light_demo.py 
pygame 2.1.2 (SDL 2.0.16, Python 3.6.9)
Hello from the pygame community. https://www.pygame.org/contribute.html
median_RT = 0.2; std = 0.072; N = 10
----

https://pypi.org/project/uldaq/

 # export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/home/abel/luna_habit/libuldaq-1.2.1/src/.libs/" 

abel@abel-XPS-13-9380:~/luna_habit/RTBox/python$ python3 RTBox_light_demo.py 
pygame 2.1.2 (SDL 2.0.16, Python 3.6.9)
Hello from the pygame community. https://www.pygame.org/contribute.html
Please run box.clockRatio() for better accurracy
median_RT = 0.26; std = 0.77; N = 4, 


----
from uldaq import (get_daq_device_inventory, DaqDevice, InterfaceType, AiInputMode, Range, AInFlag) devices = get_daq_device_inventory(InterfaceType.USB)
Segmentation fault (core dumped)
----


Libraries have been installed in:
   /usr/local/lib

If you ever happen to want to link against installed libraries
in a given directory, LIBDIR, you must either use libtool, and
specify the full pathname of the library, or use the `-LLIBDIR'
flag during linking and do at least one of the following:
   - add LIBDIR to the `LD_LIBRARY_PATH' environment variable
     during execution
   - add LIBDIR to the `LD_RUN_PATH' environment variable
     during linking
   - use the `-Wl,-rpath -Wl,LIBDIR' linker flag
   - have your system administrator add LIBDIR to `/etc/ld.so.conf'



-----

% need audio able plugged in! looking at digitial io
% http://psychtoolbox.org/docs/Daq
hid = DaqFind;
DaqDConfigPort(hid,0,0);
DaqDOut(hid,0,0);
DaqDOut(hid, 0, 100)

-----
el@abel-XPS-13-9380:~/luna_habit$ gdb python3
GNU gdb (Ubuntu 8.1-0ubuntu3) 8.1.0.20180409-git
Copyright (C) 2018 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.  Type "show copying"
and "show warranty" for details.
This GDB was configured as "x86_64-linux-gnu".
Type "show configuration" for configuration details.
For bug reporting instructions, please see:
<http://www.gnu.org/software/gdb/bugs/>.
Find the GDB manual and other documentation resources online at:
<http://www.gnu.org/software/gdb/documentation/>.
For help, type "help".
Type "apropos word" to search for commands related to "word"...
Reading symbols from python3...(no debugging symbols found)...done.
(gdb) run  testdaq.py
Starting program: /usr/bin/python3 testdaq.py
[Thread debugging using libthread_db enabled]
Using host libthread_db library "/lib/x86_64-linux-gnu/libthread_db.so.1".
[New Thread 0x7ffff419d700 (LWP 7434)]
[New Thread 0x7ffff399c700 (LWP 7435)]

Thread 1 "python3" received signal SIGSEGV, Segmentation fault.
0x00007ffff4e31185 in hid_flush_input_pipe () from /usr/local/lib/libuldaq.so.1
(gdb) backtrace
#0  0x00007ffff4e31185 in hid_flush_input_pipe ()
   from /usr/local/lib/libuldaq.so.1
#1  0x00007ffff4e21fe2 in ul::HidDaqDevice::findDaqDevices() ()
   from /usr/local/lib/libuldaq.so.1
#2  0x00007ffff4dfaaea in ul::UlDaqDeviceManager::getDaqDeviceInventory(DaqDeviceInterface) () from /usr/local/lib/libuldaq.so.1
#3  0x00007ffff4e0f8e0 in ulGetDaqDeviceInventory ()
   from /usr/local/lib/libuldaq.so.1
#4  0x00007ffff6420dae in ffi_call_unix64 ()
   from /usr/lib/x86_64-linux-gnu/libffi.so.6
#5  0x00007ffff642071f in ffi_call ()
   from /usr/lib/x86_64-linux-gnu/libffi.so.6
#6  0x00007ffff66635a4 in _ctypes_callproc ()
   from /usr/lib/python3.6/lib-dynload/_ctypes.cpython-36m-x86_64-linux-gnu.so
#7  0x00007ffff6663d34 in ?? ()
   from /usr/lib/python3.6/lib-dynload/_ctypes.cpython-36m-x86_64-linux-gnu.so
#8  0x00000000005aa9ac in _PyObject_FastCallKeywords ()
#9  0x000000000050af03 in ?? ()
#10 0x000000000050c924 in _PyEval_EvalFrameDefault ()
#11 0x0000000000508675 in ?? ()
#12 0x000000000050a3e0 in ?? ()
#13 0x000000000050adcd in ?? ()
#14 0x000000000050c924 in _PyEval_EvalFrameDefault ()
---Type <return> to continue, or q <return> to quit---
#15 0x0000000000508675 in ?? ()
#16 0x000000000050b7d3 in PyEval_EvalCode ()
#17 0x00000000006352e2 in ?? ()
#18 0x0000000000635397 in PyRun_FileExFlags ()
#19 0x0000000000638b4f in PyRun_SimpleFileExFlags ()
#20 0x00000000006396f1 in Py_Main ()
#21 0x00000000004b0e50 in main ()



