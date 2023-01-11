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
import os
import os.path


class Hardware():
    """
    wrapper for sending ttl. DAQ or psycyhopy.parallel
    """
    def __init__(self, verbose=False):
        self.verbose = verbose
        self.start  = datetime.datetime.now()

    def send(self, ttl, zero=True):
        if not self.print_timestamp(ttl):
            print(f"sending {ttl} @ {datetime.datetime.now()}")
        if zero:
            self.wait_and_zero()

    def print_timestamp(self, ttl):
        "timestamp non-zero ttl reltaive to 128 start code"
        if not self.verbose:
            return False
        # dont want to print timing of the zeros?
        if ttl==0:
            return True
        now = datetime.datetime.now()
        if ttl == 128:
            self.start = now
        diff = now - self.start
        print(f"{ttl} {diff.total_seconds():.03f}")
        return True

    def wait_and_zero(self):
        # TODO: more percise sleeping psychopy.core.wait(.005) or psychtoolbox.WaitSecs(.005)
        time.sleep(.002)  # wait 2ms and send zero. not precise enough?
        self.send(0, False)
        #  if we're lucky setData zero's pins that aren't used
        # self.port.setData(0)


# LPT + Cedrus == Loeffler EEG
class LPT(Hardware):
    """ set TTL on parallel port (LPT) """
    def __init__(self, address, verbose=False):
        from psychopy import parallel
        self.port = parallel.ParallelPort(address=address)
        self.verbose = verbose
        self.start = datetime.datetime.now()

    def send(self, ttl, zero=True):
        self.port.setData(ttl)
        self.print_timestamp(ttl)
        # without this, mne_python has a hard time finding values
        if zero:
            self.wait_and_zero()


class TTL_Logger(Hardware):
    """ log ttl code instead of sending to recording computer"""
    def __init__(self, verbose=True):
        tstr = datetime.datetime.now().strftime("%Y-%m-%dT%H:%M.%S")
        if not os.path.exists("log"):
            os.makedirs("log")
        # NB. never closed!
        self.fid = open(f"log/{tstr}_ttllog.txt", "w")
        self.verbose = verbose

    def send(self, ttl, zero=True):
        self.print_timestamp(ttl)
        self.fid.write(f"{datetime.datetime.now()}\t{ttl}\n")


class DAQ(Hardware):
    """
    using USB 1208FS
    see https://github.com/wjasper/Linux_Drivers

    NB!! binary on (>=128) or off (<128)
    DOES NOT encode 0-255, but 0/1
    """
    def __init__(self, verbose=False, daq_path='/home/lncd/luna_habit/usb1208fs-linux-drivers/USB/python'):
        # git clone https://github.com/wjasper/Linux_Drivers.git /home/lncd/luna_habit/usb1208fs-linux-drivers/
        # make install usbs, add udevrules
        sys.path.insert(0, daq_path)
        from usb_1208FS import usb_1208FS
        self.verbose = verbose
        self.dev = usb_1208FS()
        #print('wMaxPacketSize =', self.dev.wMaxPacketSize)
        self.dev.DConfig(self.dev.DIO_PORTA, self.dev.DIO_DIR_OUT)
        self.dev.DOut(self.dev.DIO_PORTA, 0)

    def send(self, ttl, zero=True):
        actual_ttl = 250  # always high
        self.dev.DOut(self.dev.DIO_PORTA, actual_ttl)
        # zero
        # always zero. we'll only ever send hi
        #self.dev.DOut(self.dev.DIO_PORTA, 0)
        if zero:
            self.wait_and_zero()


# ## BUTTON BOXES ##
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

        if response['pressed']:
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
    def __init__(self, hw, kb=None, verbose=False, lib='/home/lncd/luna_habit/RTBox_py'):
        self.run = True
        self.verbose = True
        self.kb = kb
        self.hw = hw
        self.keys = ['1', '2', '3', '4', 'S', 'L', '5', 'A']
        # numbers as they are on the button box instead of as an array
        self.sendLUR = {1:Key.left, 2:Key.up, 3:Key.up, 4:Key.right}

        # git clone https://github.com/xiangruili/RTBox_py /home/lncd/luna_habit/RTBox_py/
        sys.path.insert(0, lib)  # or PYTHONPATH=~/luna_habit/RTBox/python
        import RTBox
        box = RTBox.RTBox()
        box.enable('light')
        box.info()
        box.threshold(4)
        print(f"using threshold {box.threshold()} (1-4)")
        box.close() # switches to serial mode
        self.box = box

        _ser = box._ser
        _ser.open()
        _ser.write(b'x')  # simple mode: 1 byte per event
        _ser.write(bytearray([101, 0b111101]))  # e, all except release
        _ser.read(1)
        self._ser = _ser

    def trigger(self, index):
        # buttons send 2-4, light is 1
        # index is 0-based but keys start at 1. so add one to what we send if in range
        key = None
        ttl = None
        if index >= 5:
            self.hw.send(1)
            ttl = 250
            self.box.disable("light")
        elif index >= 0 and index < 4:
            key = self.sendLUR.get(index+1, None)
            # does't actually matter what we send. DAQ will send high
            # but update here so verbose printing is consistant
            ttl = 250 # was index + 1, but only have high and low. so always send high. will e 0'ed
            self.hw.send(ttl)
            self.kb.push(key)
            # after a key push reset the trial. should be black screen
            self.box.enable("light")
            # also consider
            #self.box.clear()


        if self.verbose:
            response = self.keys[index]
            print(f"have: {response} (i: {index}). send key {key} and ttl {ttl}")


    async def watch(self):
        while self.run:
            res = self._ser.read(1)
            b = None
            if res:
                b = bin(ord(res))[::-1]

            # too verbose
            #if self.verbose:
            #    print(res)

            # do we actually want to ignore?
            # we can work directly on the bits? -- maybe just pull the first
            # dont want to miss a PD trigger b/c a button was pushed
            # also... how fast is counting and finding?
            if b and b.count('1') == 1:  # ignore if +1 bits set
                self.trigger(b.find('1'))

            await asyncio.sleep(.0001)


class KB():
    def __init__(self):
        self._kb = Controller()  # make key press and release

    def push(self, k):
        self._kb.press(k)
        self._kb.release(k)


class HttpTTL(RequestHandler):
    """ http server (tornado request handler)
    recieve CORS get from task and translate to TTL to record in stim channel
    """
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

    print("# listening to forward ttl on http://127.0.0.1:8888")
    app = Application([('/(.*)', HttpTTL, dict(hardware=this_hardware))])
    server = HTTPServer(app)
    server.listen(8888)


async def loeffeeg(verbose=False):
    hw = LPT(address=0xD010, verbose=verbose)
    kb = KB()
    rb = Cedrus(hw, kb)
    http_run(hw)
    await asyncio.create_task(rb.watch())


async def seeg(verbose=False):
    hw = DAQ(verbose=verbose)
    kb = KB()
    rb = RTBox(hw, kb, verbose)

    logger = TTL_Logger(verbose=verbose)
    http_run(logger)
    await asyncio.create_task(rb.watch())


async def rtbox_test(verbose=False):
    "no http server, no DAQ. just RTBox with generic hardware class"
    hw = Hardware(verbose=verbose)
    kb = KB()
    rb = RTBox(hw, kb, verbose)
    await asyncio.create_task(rb.watch())


async def fakeeeg(usekeyboard=False, verbose=False):
    hw = Hardware(verbose=verbose)
    kb = KB()
    http_run(hw)
    rb = FakeButton(hw, kb)
    # kludge. disable trigger function so we dont send the 'a' key every 5 seconds
    if not usekeyboard:
        rb.trigger = lambda a: 1
    # need this await or we'll exit as soon as we send the first trigger
    await asyncio.create_task(rb.watch())


def parser(args):
    import argparse
    p = argparse.ArgumentParser(description="Intercept http queries and watch ButtonBox/PhotoDiode")
    p.add_argument('place', choices=["loeff", "seeg", "test", "test_rtbox"], help='where (also how) to use button and ttl')
    p.add_argument('-k','--keyboard', help='use keyboard (only for "test")', action='store_true', dest="usekeyboard")
    p.add_argument('-v','--verbose', help='additonal printing', action='store_true', dest="verbose")
    return p.parse_args(args)


if __name__ == "__main__":
    args = parser(sys.argv[1:])
    print(f"# *{args.place}* python pd+button+ttl bridge")
    if args.place == "loeff":
        asyncio.run(loeffeeg(verbose=args.verbose))
    elif args.place == "seeg":
        asyncio.run(seeg(verbose=args.verbose))
    elif args.place == "test":
        asyncio.run(fakeeeg(args.usekeyboard, verbose=args.verbose))
    elif args.place == "test_rtbox":
        asyncio.run(rtbox_test(verbose=args.verbose))
    else:
        print(f"unkown place '{args.place}'! -- argparse should have caught this")
