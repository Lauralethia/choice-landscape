#!/usr/bin/env python3
"""
http server to send ttls
task javascript -> POST /123 -> hardware ttl

run task with with url_tweak:
    http://localhost:9500/#landscape=ocean&timing=debug&noinstruction&ttl=local
    file:///home/foranw/src/tasks/choice-landscape/out/index.html#noinstructions&landscape=ocean&ttl=local

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

import sys
import asyncio
import datetime
import time
from tornado.httpserver import HTTPServer
from tornado.web import RequestHandler, Application
from tornado.ioloop import IOLoop
from pynput.keyboard import Controller, Key

class Hardware():
    """
    wrapper for sending ttl. DAQ or psycyhopy.parallel
    """
    def send(self, ttl, zero=True):
        print(f"sending {ttl} @ {datetime.datetime.now()}")
        if zero:
            self.wait_and_zero()

    def wait_and_zero(self):
        # TODO: more percise sleeping psychopy.core.wait(.005) or psychtoolbox.WaitSecs(.005)
        time.sleep(.002)  # wait 2ms and send zero. not precise enough?
        self.send(0, False)
        #  if we're lucky setData zero's pins that aren't used
        # self.port.setData(0)


# LPT + Cedrus == Loeffler EEG
class LPT(Hardware):
    """ set TTL on parallel port (LPT) """
    def __init__(self, address):
        from psychopy import parallel
        import time
        self.port = parallel.ParallelPort(address=address)

    def send(self, ttl, zero=True):
        self.port.setData(ttl)
        # without this, mne_python has a hard time finding values
        if zero:
            self.wait_and_zero()


class DAQ(Hardware):
    def __init__():
        import usb1208FS
        pass
    def send_todo(ttl, zero=True):
        usb1208FS.DOut(usb1208FS.DIO_PORTA, ttl)
        # TODO: do we need to zero?
        if zero:
            self.wait_and_zero()



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
    """ cedrus response box (RB-x40)
    top 3 right buttons are 5, 6, 7 (0-2 left, 3,4 thumb 5-7 right)"""
    def __init__(self, hw, kb):
        import pyxid2
        self.run = True
        self.hw = hw
        self.kb = kb
        self.dev = pyxid2.get_xid_devices()[0]
        self.dev.reset_base_timer()
        self.dev.reset_rt_timer()
        self.resp_to_key = {5: Key.left, 6: Key.up, 7: Key.right, 4: Key.down}
        self.light_ttl = 1
        self.resp_to_ttl = {5: 2, 6: 3, 7: 4}
        # <XidDevice "Cedrus RB-840">

    def trigger(self, response):
        """ todo response number to ttl value translation
        port 0 is button box
        port 2 is photodiode/light sensor. always release. only when bright from dark
        """
        # {'port': 0, 'key': 7, 'pressed': True, 'time': 13594}
        # {'port': 2, 'key': 3, 'pressed': True, 'time': 3880}

        if response['pressed'] == True:
            if response['port'] == 2:
                self.hw.send(self.light_ttl)
            if self.kb and response['port'] == 0:
                rk = response.get("key")
                ttl = self.resp_to_ttl.get(rk)
                key = self.resp_to_key.get(rk)
                if ttl:
                    self.hw.send(ttl)
                if key:
                    self.kb.push(key)
                print(f"pushed ttl: {ttl}; key: {key}; resp: {response}")
                sys.stdout.flush()

    async def watch(self):
        while self.run:
            while not self.dev.has_response():
                self.dev.poll_for_response()
                await asyncio.sleep(.0001)
            resp = self.dev.get_next_response()
            self.trigger(resp)


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
        self.hardware.send(int(ttl))
        # TODO: return string. no need to do interpolation
        self.write(f"ttl: {ttl} @ global {datetime.datetime.now()}")

def http_run(this_hardware):

    # 128 starts recording in loeff eeg
    # this should be done by task?
    #this_hardware.send(128)

    app = Application([('/(.*)', HttpTTL, dict(hardware=this_hardware))])
    server = HTTPServer(app)
    server.listen(8888)

async def loeffeeg():
    hw = LPT(address=0xD010)
    kb = KB()
    rb = Cedrus(hw, kb)
    http_run(hw)
    await asyncio.create_task(rb.watch())

async def fakeeeg(usekeyboard=False):
    hw = Hardware()
    kb = KB()
    http_run(hw)
    rb = FakeButton(hw,kb)
    # kludge. disable trigger function so we dont send the 'a' key every 5 seconds
    if not usekeyboard:
        rb.trigger = lambda a: 1
    # need this await or we'll exit as soon as we send the first trigger
    await asyncio.create_task(rb.watch())

def parser(args):
    import argparse
    p = argparse.ArgumentParser(description="Intercept http queries and watch ButtonBox/PhotoDiode")
    p.add_argument('place', help='one of: "loeff","test","seeg"')
    p.add_argument('-k','--keyboard', help='use keyboard (only for testing)', action='store_true', dest="usekeyboard")
    return p.parse_args(args)


if __name__ == "__main__":
    args = parser(sys.argv[1:])
    print(args.place)
    if args.place == "loeff":
        asyncio.run(loeffeeg())
    elif args.place == "seeg":
        print("NOT IMPLEMENTED!")
    elif args.place == "test":
        asyncio.run(fakeeeg(args.usekeyboard))
    else:
        print(f"unkown place '{args.place}'!")

