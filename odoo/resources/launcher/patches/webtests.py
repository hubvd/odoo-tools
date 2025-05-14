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


def _spawn_chrome(self, cmd):
    extra_flags = [
        "--unsafely-disable-devtools-self-xss-warnings",
        "--remote-allow-origins=*",
    ]
    for flag in extra_flags:
        if flag not in cmd:
            cmd.insert(len(cmd) - 1, flag)
    try:
        if idx := cmd.index("--start-maximized"):
            cmd[idx] = "--start-fullscreen"
    except ValueError:
        pass
    return (self, cmd), {}


def start_tour(*args, **kwargs):
    if os.environ.get("ODOO_TOUR_STEP_DELAY"):
        kwargs["step_delay"] = int(os.environ.get("ODOO_TOUR_STEP_DELAY"))
    if os.environ.get("ODOO_WATCH_CHROME") == "1":
        kwargs["watch"] = True
    if os.environ.get("ODOO_DEBUG_CHROME") == "1":
        kwargs["debug"] = True
    return args, kwargs


def browser_js(*args, **kwargs):
    if os.environ.get("ODOO_WATCH_CHROME") == "1":
        kwargs["watch"] = True
    if os.environ.get("ODOO_DEBUG_CHROME") == "1":
        kwargs["debug"] = True
    return args, kwargs


class WebTests:
    def apply(self):
        patch_arguments(ChromeBrowser, "_spawn_chrome", _spawn_chrome)
        patch_arguments(HttpCase, "browser_js", browser_js)
        patch_arguments(HttpCase, "start_tour", start_tour)
