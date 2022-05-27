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

class HelloWorld(RequestHandler):
    """Print 'Hello, world!' as the response body."""

    def get(self):
        """Handle a GET request for saying Hello World!."""
        self.write(f"Hello, world! {GLOBAL_I}")

async def rtbox_wait():
    global GLOBAL_I
    while True:
        print(datetime.datetime.now())
        GLOBAL_I=GLOBAL_I+1
        await asyncio.sleep(1)

def http_run():
    app = Application([('/', HelloWorld)])
    server = HTTPServer(app)
    server.listen(8888)

async def main():
    http_run()
    await asyncio.create_task(rtbox_wait())

asyncio.run(main())
