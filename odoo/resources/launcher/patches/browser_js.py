import inspect
import os
import time

try:
    from concurrent.futures import InvalidStateError
except ImportError:
    InvalidStateError = NotImplementedError

import werkzeug.urls

from odoo.tests.common import ChromeBrowserException, _logger, HOST


def browser_js_ge_16_4(
    self,
    url_path,
    code,
    ready="",
    login=None,
    timeout=60,
    cookies=None,
    error_checker=None,
    watch=False,
    **kw,
):
    if os.environ.get("QUNIT_WATCH") == "1":
        watch = True

    if not self.env.registry.loaded:
        self._logger.warning("HttpCase test should be in post_install only")

    if any(
        f.filename.endswith("/coverage/execfile.py")
        for f in inspect.stack()
        if f.filename
    ):
        timeout = timeout * 1.5

    self.start_browser()
    if watch and self.browser.dev_tools_frontend_url:
        _logger.warning(
            "watch mode is only suitable for local testing - increasing tour timeout to 3600"
        )
        timeout = max(timeout * 10, 3600)
        debug_front_end = f"http://127.0.0.1:{self.browser.devtools_port}{self.browser.dev_tools_frontend_url}"
        self.browser._chrome_without_limit([self.browser.executable, debug_front_end])
        time.sleep(3)
    try:
        self.authenticate(login, login)
        self.cr.flush()
        self.cr.clear()
        url = werkzeug.urls.url_join(self.base_url(), url_path)
        if watch:
            parsed = werkzeug.urls.url_parse(url)
            qs = parsed.decode_query()
            qs["watch"] = "1"
            url = parsed.replace(query=werkzeug.urls.url_encode(qs)).to_url()
        self._logger.info('Open "%s" in browser', url)

        if self.browser.screencasts_dir:
            self._logger.info("Starting screencast")
            self.browser.start_screencast()
        if cookies:
            for name, value in cookies.items():
                self.browser.set_cookie(name, value, "/", HOST)

        self.browser.navigate_to(url, wait_stop=not bool(ready))
        ready = ready or "document.readyState === 'complete'"
        self.assertTrue(
            self.browser._wait_ready(ready),
            'The ready "%s" code was always falsy' % ready,
        )

        error = False
        try:
            self.browser._wait_code_ok(code, timeout, error_checker=error_checker)
        except ChromeBrowserException as chrome_browser_exception:
            error = chrome_browser_exception
        if error:
            if code:
                message = 'The test code "%s" failed' % code
            else:
                message = "Some js test failed"
            self.fail("%s\n\n%s" % (message, error))

    finally:
        self.browser.delete_cookie("session_id", domain=HOST)
        self.browser.clear()
        self._wait_remaining_requests()
