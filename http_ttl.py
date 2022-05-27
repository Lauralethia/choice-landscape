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

GLOBAL_I = 0
class Hardware():
    """
    wrapper for sending ttl
    """
    def send(self, ttl):
        print(f"sending {ttl} @ {datetime.datetime.now()}")

class HttpTTL(RequestHandler):
    """ http server to send TTL  """
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

def http_run():
    this_hardware=Hardware()
    this_hardware.send("test start")
    app = Application([('/(.*)', HttpTTL, dict(hardware=this_hardware))])
    server = HTTPServer(app)
    server.listen(8888)

async def main():
    http_run()
    await asyncio.create_task(rtbox_wait())

asyncio.run(main())
