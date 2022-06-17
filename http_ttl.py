#!/usr/bin/env python
"""
http server to send ttls
task javascript -> POST /123 -> hardware ttl

unknowns:
 * CORS issue if using heroku?
 * terrible timing?
 * async? might want to be watching for button pushes here too?
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

GLOBAL_I = 0
class Hardware():
    """
    wrapper for sending ttl. DAQ or psycyhopy.parallel
    """
    def send(self, ttl):
        print(f"sending {ttl} @ {datetime.datetime.now()}")

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
        self.write(f"ttl: {ttl} @ global {GLOBAL_I}")
        self.hardware.send(ttl)

async def rtbox_wait():
    global GLOBAL_I
    while True:
        print(datetime.datetime.now())
        GLOBAL_I=GLOBAL_I+1
        await asyncio.sleep(1)

def http_run(this_hardware):
    this_hardware.send("test start")
    app = Application([('/(.*)', HttpTTL, dict(hardware=this_hardware))])
    server = HTTPServer(app)
    server.listen(8888)

async def main():
    hw = Hardware()
    kb = KB()
    rb = Cedrus(hw, kb)
    http_run(hw)
    await asyncio.create_task(rb.watch())
    #await asyncio.create_task(rtbox_wait())

asyncio.run(main())
