#!/usr/bin/env python
import asyncio
import sys
from tornado.httpserver import HTTPServer
from tornado.web import RequestHandler, Application

class HttpTTL(RequestHandler):
    def get(self, msg):
        self.write(f"{msg}")

def http_run():
    app = Application([('/(.*)', HttpTTL)])
    server = HTTPServer(app)
    server.listen(4444)

async def watch():
    while True:
        await asyncio.sleep(5)
        print("5 sec trigger")
        sys.stdout.flush()

async def main():
    http_run()
    await asyncio.create_task(watch())

asyncio.run(main())
