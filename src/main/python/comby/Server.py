#!/usr/bin/env python3
"""
Very simple HTTP server in python for logging requests
Usage::
    ./server.py [<port>]
"""
from http.client import OK
from http.server import BaseHTTPRequestHandler, HTTPServer
import logging
import json

from Mappings import adapt, match_to_meta_pattern


class S(BaseHTTPRequestHandler):
    def _set_response(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])  # <--- Gets the size of data
        post_data = self.rfile.read(content_length)  # <--- Gets the data itself
        source = self.headers['source']
        target = self.headers['target']
        # source = self.headers['old_source']

        logging.info("POST request,\nPath: %s\nHeaders:\n%s\n\nBody:\n%s\n",
                     str(self.path), str(self.headers), post_data.decode('utf-8'))

        self._set_response()

        new_source = match_to_meta_pattern(source, target, {})
        response = {"Response": new_source}
        self.wfile.write(json.dumps(response).encode('utf-8'))

    # def do_POST(self):
    #     content_length = int(self.headers['Content-Length'])  # <--- Gets the size of data
    #     post_data = self.rfile.read(content_length)  # <--- Gets the data itself
    #     from_type = self.headers['from_type']
    #     to_type = self.headers['to_type']
    #     source = self.headers['old_source']
    #
    #     logging.info("POST request,\nPath: %s\nHeaders:\n%s\n\nBody:\n%s\n",
    #                  str(self.path), str(self.headers), post_data.decode('utf-8'))
    #
    #     self._set_response()
    #     new_source = adapt((from_type, to_type), source)
    #     self.wfile.write(new_source.encode('utf-8'))


def run(server_class=HTTPServer, handler_class=S, port=8080):
    logging.basicConfig(level=logging.INFO)
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    logging.info('Starting httpd...\n')
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    logging.info('Stopping httpd...\n')


if __name__ == '__main__':
    from sys import argv

    if len(argv) == 2:
        run(port=int(argv[1]))
    else:
        run()
