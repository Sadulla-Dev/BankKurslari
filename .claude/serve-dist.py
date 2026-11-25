# Local preview of web/dist that answers like Cloudflare's static assets: /pul-otkazmalari serves
# pul-otkazmalari.html, /ru/ serves ru/index.html, unknown addresses get dist/404.html with status 404.
#   python3 .claude/serve-dist.py 8789
import http.server
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "web", "dist")


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)

    def send_head(self):
        path = self.path.split("?", 1)[0].split("#", 1)[0]
        local = self.translate_path(path)
        if not os.path.exists(local) and os.path.isfile(local + ".html"):
            self.path = path + ".html"
        elif not os.path.exists(local):
            self.send_response(404)
            body = open(os.path.join(ROOT, "404.html"), "rb").read()
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return None
        return super().send_head()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8789
    http.server.ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
