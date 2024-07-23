#!/usr/bin/env python3
import time
import sys
import signal
from pynput.keyboard import Controller
sys.path.insert(0,'/home/luna/luna_habit/RTBox_py') # or PYTHONPATH=~/luna_habit/RTBox/python
import RTBox

# this would run in background. and exit (killing background). ideal for running in e.g ipython
#from RTBoxAsKeypad import RTBoxAsKeypad
#kbd = RTBoxAsKeypad() 
#kbd.box.enable('light')

# ripped off from there
# The four triggers are 1-4, 5, S,     L,     A 
#                           TR, sound, light, aux 

class Killer:
  """use ctrl-c to kill"""
  kill = False
  def __init__(self):
    signal.signal(signal.SIGINT, self.set_kill)
    signal.signal(signal.SIGTERM, self.set_kill)

  def set_kill(self, *args):
    self.kill = True


## DAQ
# https://pypi.org/project/uldaq/

class Daq:
    def __init__(self):
        from uldaq import (get_daq_device_inventory, DaqDevice, InterfaceType, AiInputMode, Range, AInFlag)
        devices = get_daq_device_inventory(InterfaceType.USB)
        # Create a DaqDevice Object and connect to the device
        daq_device = DaqDevice(devices[0])
        daq_device.connect()
        ai_device = daq_device.get_ai_device()
        ai_info = ai_device.get_info()

        self.dev = daq_device

    def close(self):
        self.dev.disconnect()
        self.dev.release()

    def send(self, ttl):
        self.dev.d_out(ttl)

## RTBOX

interval = .008 # 8ms is fastest os can deal with keys anyway
keys = ['1', '2', '3', '4', 'S', 'L', '5', 'A']
_kb = Controller() # make key press and release

box = RTBox.RTBox()
box.enable('light')
box.info()
box.threshold(4)
print(f"using threshold {box.threshold()} (1-4)")
box.close()

_ser = box._ser
_ser.open()
_ser.write(b'x') # simple mode: 1 byte per event
_ser.write(bytearray([101, 0b111101])) # e, all except release
_ser.read(1)

sig = Killer()
while not sig.kill:
    # nothing to do if no events have accumulated
    if not _ser.in_waiting:
        continue

    b = bin(ord(_ser.read(1)))[::-1]

    # do we actually want to ignore?
    # we can work directly on the bits?
    if b.count('1')==1: # ignore if +1 bits set
        k = keys[b.find('1')]
        print(f"# respones! {k}")
        if k == "L":
            # need to reneable trigger
            print("PHOTODIODE TRIGGER!")
            box.disable('light'); box.enable('light')
            #box.enable('light')
        # maybe dont need else
        # can send 'l' if we wanted?
        else:
            _kb.press(k)
            _kb.release(k)

    # not sure how percise time.sleep does
    # could use psychopy.core.wait?
    time.sleep(interval)

print("Shutting down")
_ser.close()
