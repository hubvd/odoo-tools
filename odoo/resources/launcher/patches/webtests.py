import os
import time
import logging

from odoo.release import version_info

test_logger = logging.getLogger("odoo.tests.common")
initial_level = test_logger.getEffectiveLevel()
test_logger.setLevel(logging.CRITICAL)

from odoo.tests import HttpCase
from odoo.tests.common import ChromeBrowser

test_logger.setLevel(initial_level)

from .patch_tools import patch_arguments, side_effect

major_version = version_info[0]
if isinstance(major_version, str):
    major_version = int(major_version.lstrip("saas~"))

version = float(f"{major_version}.{version_info[1]}")


def _spawn_chrome_lt_16_4(self, cmd):
    if "--remote-allow-origins=*" not in cmd:
        cmd.insert(len(cmd) - 1, "--remote-allow-origins=*")
    return (self, cmd), {}


def _json_command(res, self, command, *args, **kwargs):
    if command == "":
        self.dev_tools_frontend_url = res.get("devtoolsFrontendUrl")


def browser_js(*args, **kwargs):
    if os.environ.get("QUNIT_WATCH") == "1":
        kwargs["watch"] = True
    return args, kwargs


@classmethod
def start_browser_lt_16(cls):
    if cls.browser is None:
        cls.browser = ChromeBrowser(cls._logger, cls.browser_size, cls.__name__)
        cls.addClassCleanup(cls.terminate_browser)
    if os.environ.get("QUNIT_WATCH") == "1":
        debug_front_end = f"http://127.0.0.1:{cls.browser.devtools_port}{cls.browser.dev_tools_frontend_url}"
        cls.browser._spawn_chrome([cls.browser.executable, debug_front_end])
        time.sleep(3)


def _wait_code_ok_lt_16(self, code, timeout):
    if os.environ.get("QUNIT_WATCH") == "1":
        timeout = max(timeout * 10, 3600)
    return (self, code, timeout), {}


def start_tour(*args, **kwargs):
    if os.environ.get("STEP_DELAY"):
        kwargs["step_delay"] = int(os.environ.get("STEP_DELAY"))
    return args, kwargs


def _open_websocket(res, self):
    self.dev_tools_frontend_url = self._json_command("")[0]["devtoolsFrontendUrl"]


def init_chrome(self, *args, **kwargs):
    self.original_headless = kwargs.get("headless", True)
    if not self.original_headless:
        kwargs["headless"] = True

    return (self, *args), kwargs


def post_init_chrome(res, self, *args, **kwargs):
    if not self.original_headless:  # watch mode
        debug_front_end = (
            f"http://127.0.0.1:{self.devtools_port}{self.dev_tools_frontend_url}"
        )
        self._chrome_without_limit([self.executable, debug_front_end])
        time.sleep(3)


class WebTests:
    def apply(self):
        # Add --remote-allow-origins to chrome
        patch_arguments(ChromeBrowser, "_spawn_chrome", _spawn_chrome_lt_16_4)

        # Backport the watch option before 16
        if version < 16:
            side_effect(ChromeBrowser, "_json_command", _json_command)
            patch_arguments(ChromeBrowser, "_wait_code_ok", _wait_code_ok_lt_16)
            HttpCase.start_browser = start_browser_lt_16

        # Override watch if QUNIT_WATCH is set
        if version >= 16:
            patch_arguments(HttpCase, "browser_js", browser_js)

        # Override step_delay if STEP_DELAY is set
        patch_arguments(HttpCase, "start_tour", start_tour)

        if version >= 16.4:
            return
            # Bring back Chrome remote view
            side_effect(ChromeBrowser, "_open_websocket", _open_websocket)
            patch_arguments(ChromeBrowser, "__init__", init_chrome)
            side_effect(ChromeBrowser, "__init__", post_init_chrome)
