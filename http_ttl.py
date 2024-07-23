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
from pynput.keyboard import Controller, Key, Listener
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

    def wait_and_zero(self, wait=.002):
        # TODO: more percise sleeping psychopy.core.wait(.005) or psychtoolbox.WaitSecs(.005)
        time.sleep(wait)  # wait 2ms and send zero. not precise enough?
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
    def __init__(self, verbose=False, daq_path='/home/luna/luna_habit/usb1208fs-linux-drivers/USB/python'):
        # git clone https://github.com/wjasper/Linux_Drivers.git /home/luna/luna_habit/usb1208fs-linux-drivers/
        # make install usbs, add udevrules
        sys.path.insert(0, daq_path)
        from usb_1208FS import usb_1208FS
        self.verbose = verbose
        self.dev = usb_1208FS()
        #print('wMaxPacketSize =', self.dev.wMaxPacketSize)
        self.dev.DConfig(self.dev.DIO_PORTA, self.dev.DIO_DIR_OUT)
        self.dev.DOut(self.dev.DIO_PORTA, 0)

    def send(self, ttl, zero=True):
        # always high 250. unless 0 (low)
        actual_ttl = 250 if ttl > 0 else 0
        self.dev.DOut(self.dev.DIO_PORTA, actual_ttl)
        # zero
        # always zero. we'll only ever send hi
        #self.dev.DOut(self.dev.DIO_PORTA, 0)
        if zero:
            # to ID start and end, wait twice as long
            if ttl in [128,129]:
                wait=.005
            else:
                wait=.002
            self.wait_and_zero(wait=wait)


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


class KeyboardListener():
    """ watch a real keyboard (cf. Cedrus, RTBox)
    send ttl if approprate key pushed
    left,right,up to ttl 2,3,4 matching cedrus (as of 20240510)
    (1 reserved for photodiode)
    """
    def __init__(self, hw, verbose=False):
        self.hw = hw
        self.verbose = verbose
        self.keys_ttl = {'left': 2,'right': 3, 'up': 4}
        self.keys = self.keys_ttl.keys()

    def parse_key(self, key):
        "send key if key is in "
        try:
            k = key.name  # 'left', 'right', 'up'
        except AttributeError:
            k = key.char   # single-char keys 'a'

        if k in self.keys:
            ttl = self.keys_ttl[k]
            self.hw.send(ttl)

        if self.verbose:
            print(f"[{datetime.datetime.now()}] {k} pushed")

    async def watch(self):
        "watch keypresses in background. forward key on to see if button should be pushed"
        listener = Listener(on_press=self.parse_key)
        listener.start()
        listener.join()


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
    def __init__(self, hw, kb=None, verbose=False, lib='/home/luna/luna_habit/RTBox_py'):
        self.run = True
        self.verbose = True
        self.kb = kb
        self.hw = hw
        self.keys = ['1', '2', '3', '4', 'S', 'L', '5', 'A']
        # numbers as they are on the button box instead of as an array
        self.sendLUR = {1:Key.left, 2:Key.up, 3:Key.up, 4:Key.right}

        # git clone https://github.com/xiangruili/RTBox_py /home/luna/luna_habit/RTBox_py/
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
            self.hw.send(1) # 1 for eeg, but 'high' for seeg
            ttl = 250
            self.box.disable("light")
            self.box.enable("light") # WARNING: this is against the suggestion. might be noisy PD
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
    """settings for Luna Lab EEG in Loeffler building
    uses Cedrus response box (+attached photodiode)
    and TTL over LPT (biosemi)
    """
    hw = LPT(address=0xD010, verbose=verbose)
    kb = KB()
    rb = Cedrus(hw, kb)
    http_run(hw)
    await asyncio.create_task(rb.watch())


async def ar_eeg(verbose=False):
    """
    button pushes directly from they keyboard.
    TTL over LPT (biosemi)
    20240510WF - init for LAF
    """
    port = 0xCFE8
    hw = LPT(address=port, verbose=verbose)
    rb = KeyboardListener(hw, verbose=verbose)
    http_run(hw)
    await asyncio.create_task(rb.watch())


async def seeg(verbose=False):
    """
    sEEG at childrens hospital using USB 1208FS (binary TTL!) attached to grapevine
    responses from RTBox with attached photodiode
    """
    hw = DAQ(verbose=verbose)
    kb = KB()
    rb = RTBox(hw, kb, verbose)

    logger = TTL_Logger(verbose=verbose)
    http_run(logger)
    await asyncio.create_task(rb.watch())


async def test_DAQ(verbose=False):
    "only test DAQ. loop forever: send high and auto reset (seeg)"
    hw = DAQ(verbose=verbose)
    while True:
        await asyncio.sleep(1)
        print("sending high and zeroing")
        hw.send(250)  # 250 just has to be non-zero

async def test_keyboard(verbose=True):
    "confirm keyboard responses are registered (for ar_eeg)"
    hw = Hardware(verbose=verbose)
    rb = KeyboardListener(hw, verbose=True)
    http_run(hw)
    print("push arrow keys. should see events here")
    await asyncio.create_task(rb.watch())


async def test_LPT(verbose=False, address=0xD010):
    "only test LPT. loop forever: send high and auto reset (loef eeg)"
    hw = LPT(address=address, verbose=verbose)
    while True:
        await asyncio.sleep(1)
        print("sending high and zeroing")
        hw.send(250)


async def rtbox_test(verbose=False):
    "no http server, no DAQ. just RTBox with generic hardware class (seeg)"
    hw = Hardware(verbose=verbose)
    kb = KB()
    rb = RTBox(hw, kb, verbose)
    print("push button box keys. should see events here")
    await asyncio.create_task(rb.watch())


async def cedrus_test(verbose=False):
    "test cedrus response button box  (loef eeg)"
    hw = Hardware(verbose=verbose)
    kb = KB()
    rb = Cedrus(hw, kb)
    http_run(hw)
    await asyncio.create_task(rb.watch())


async def fakeeeg(usekeyboard=False, verbose=False):
    "listen on port, but don't interface with DAQ or RTBox"
    hw = Hardware(verbose=verbose)
    kb = KB()
    http_run(hw)
    rb = FakeButton(hw, kb)
    # kludge. disable trigger function so we dont send the 'a' key every 5 seconds
    if not usekeyboard:
        rb.trigger = lambda a: 1

    print("listening for ttl on http. no RTBox or DAQ")
    print("in new term try sending code: curl http://127.0.0.1:8888/1")
    # need this await or we'll exit as soon as we send the first trigger
    await asyncio.create_task(rb.watch())


def parser(args):
    import argparse
    p = argparse.ArgumentParser(description="Intercept http queries and watch ButtonBox/PhotoDiode")
    p.add_argument('place', choices=["loeff", "seeg", "areeg",
                                     "test_http", "test_keyboard",
                                     "test_rtbox", "test_DAQ",
                                     "test_cedrus", "test_lpt"],
                   help='where (also how) to use button and ttl')
    p.add_argument('-k','--keyboard', help='use keyboard (only for "test_http")', action='store_true', dest="usekeyboard")
    p.add_argument('-v','--verbose', help='additonal printing', action='store_true', dest="verbose")
    return p.parse_args(args)


if __name__ == "__main__":
    args = parser(sys.argv[1:])
    print(f"# *{args.place}* python pd+button+ttl bridge")
    if args.place == "loeff":
        asyncio.run(loeffeeg(verbose=args.verbose))
    elif args.place == "seeg":
        asyncio.run(seeg(verbose=args.verbose))
    elif args.place == "areeg":
        asyncio.run(ar_eeg(verbose=args.verbose))
    elif args.place == "seeg":
        asyncio.run(seeg(verbose=args.verbose))
    elif args.place == "test_http":
        asyncio.run(fakeeeg(args.usekeyboard, verbose=args.verbose))

    elif args.place == "test_DAQ":
        asyncio.run(test_DAQ(verbose=args.verbose))
    elif args.place == "test_rtbox":
        asyncio.run(rtbox_test(verbose=args.verbose))
    elif args.place == "test_keyboard":
        asyncio.run(test_keyboard(verbose=args.verbose))
    elif args.place == "test_cedrus":
        asyncio.run(cedrus_test(verbose=args.verbose))
    elif args.place == "test_lpt":
        asyncio.run(test_LPT(verbose=args.verbose))
    else:
        print(f"unkown place '{args.place}'! -- argparse should have caught this")
