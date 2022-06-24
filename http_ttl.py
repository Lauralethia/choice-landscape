#!/usr/bin/env python
"""
http server to send ttls
task javascript -> POST /123 -> hardware ttl

also polls for button boxes, sends ttl on push
and forwards as keypush

unknowns:
 * CORS issue if using heroku?
 * terrible timing?
 * key pushes on windows?
"""


# https://github.com/pfalcon/picoweb
# https://bottlepy.org/docs/dev/async.html
# https://www.tornadoweb.org/en/stable/guide/async.html
#  https://www.tornadoweb.org/en/stable/httpserver.html#http-server
# https://stackoverflow.com/questions/60471268/run-tornado-alongside-another-asyncio-long-running-task

import asyncio
import datetime
from tornado.httpserver import HTTPServer
from tornado.web import RequestHandler, Application
from tornado.ioloop import IOLoop
from pynput.keyboard import Controller

class Hardware():
    """
    wrapper for sending ttl. DAQ or psycyhopy.parallel
    """
    def send(self, ttl):
        print(f"sending {ttl} @ {datetime.datetime.now()}")

    def wait_and_zero(self):
        # TODO: more percise sleeping psychopy.core.wait(.005) or psychtoolbox.WaitSecs(.005)
        time.sleep(.005)  # wait 5ms and send zero. not precise enough?
        self.send(0)
        #  if we're lucky setData zero's pins that aren't used
        # self.port.setData(0)


# LPT + Cedrus == Loeffler EEG
class LPT(Hardware):
    """ set TTL on parallel port (LPT) """
    def __init__(self, address):
        from psychopy import parallel
        import time
        self.port = parallel.ParallelPort(address=address)

    def send_todo(ttl):
        self.port.setData(ttl)
        # TODO: needed?
        # self.wait_and_zero()


class DAQ(Hardware):
    def __init__():
        import usb1208FS
        pass
    def send_todo(ttl):
        usb1208FS.DOut(usb1208FS.DIO_PORTA, ttl)
        # TODO: do we need to zero?
        # self.wait_and_zero()



### BUTTON BOXES
class FakeButton():
    def __init__(self, hw, kb):
        self.hw = hw
        self.kb = kb
    def trigger(self, msg):
        self.kb.push("a")
        self.hw.send(msg)

    async def watch(self):
        while True:
            await asyncio.sleep(5)
            self.trigger("5 sec trigger")

class Cedrus():
    """ cedrus response box (RB-x40) """
    def __init__(self, hw, kb):
        import pyxid2
        self.run = True
        self.hw = hw
        self.kb = kb
        self.dev = pyxid2.get_xid_devices()[0]
        self.dev.reset_base_timer()
        self.dev.reset_rt_timer()
        # <XidDevice "Cedrus RB-840">
            
    def trigger(self, response):
        """ todo response number to ttl value translation
        port 0 is button box
        port 2 is photodiode/light sensor. always release. only when bright from dark
        """
        # {'port': 0, 'key': 7, 'pressed': True, 'time': 13594}
        # {'port': 2, 'key': 3, 'pressed': True, 'time': 3880}

        if response['pressed'] == True:
            self.hw.send(f"{response}")
            if self.kb and response['port'] == 0:
                # TODO: figure out keys to push
                self.kb.push("a")

    async def watch(self):
        while self.run:
            while not self.dev.has_response():
                self.dev.poll_for_response()
            response = self.dev.get_next_response()
            self.trigger(response)

class RTBox():
    def __init__(self, hw):
        self.run = True
        self.hw = hw
        self.keys = ['1', '2', '3', '4', 'S', 'L', '5', 'A']

        sys.path.insert(0,'/home/abel/luna_habit/RTBox_py') # or PYTHONPATH=~/luna_habit/RTBox/python
        import RTBox
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
        self._ser = _ser

    def trigger(self, index):
        response = self.keys[index]
        self.hw.send(f"{response}")


    async def watch(self):
        while self.run:
            b = bin(ord(self._ser.read(1)))[::-1]
            # do we actually want to ignore?
            # we can work directly on the bits? -- maybe just pull the first
            # dont want to miss a PD trigger b/c a button was pushed
            # also... how fast is counting and finding?
            if b.count('1')==1: # ignore if +1 bits set
                self.trigger(b.find('1'))


class KB():
    def __init__(self):
        keys = ['1', '2', '3', '4', 'S', 'L', '5', 'A']
        self._kb = Controller() # make key press and release

    def push(self, k):
        self._kb.press(k)
        self._kb.release(k)

class HttpTTL(RequestHandler):
    """ http server (tornado request handler)
    recieve CORS get from task and translate to TTL to record in stim channel"""
    # https://www.tornadoweb.org/en/stable/web.html
    def initialize(self, hardware):
        self.hardware = hardware

    def set_default_headers(self):
        self.set_header("Access-Control-Allow-Origin", "*")
        self.set_header("Access-Control-Allow-Headers", "x-requested-with")
        self.set_header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')

    def get(self, ttl):
        """Handle a GET request for saying Hello World!."""
        self.hardware.send(ttl)
        # TODO: return string. no need to do interpolation
        self.write(f"ttl: {ttl} @ global {datetime.datetime.now()}")

def http_run(this_hardware):
    this_hardware.send("test start")
    app = Application([('/(.*)', HttpTTL, dict(hardware=this_hardware))])
    server = HTTPServer(app)
    server.listen(8888)

async def main():
    hw = Hardware()
    kb = KB()
    #rb = Cedrus(hw, kb)
    rb = FakeButton(hw,kb)
    http_run(hw)
    await asyncio.create_task(rb.watch())

asyncio.run(main())
