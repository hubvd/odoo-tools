from rich.progress import (
    Progress,
    SpinnerColumn,
    TextColumn,
    BarColumn,
    TaskProgressColumn,
    TimeRemainingColumn,
)

import odoo
import unittest
import time
import logging

try:
    version = 1
    from odoo.tests.runner import OdooTestResult
except ImportError:
    version = 2
    from odoo.tests.result import OdooTestResult
from odoo.tests import HttpCase
from odoo import sql_db
import os


_logger = logging.getLogger("odoo.tests.runner")


def startTest(super):
    def decorator(self, test):
        _logger.info(
            f"Starting [bold steel_blue1 underline]{self.getDescription(test)}[/]",
            extra={"markup": True},
        )
        if version == 2:
            self.testsRun += 1
        super(self, test)
        self.time_start = time.time()
        self.queries_start = sql_db.sql_counter

    return decorator


def browser_js(super):
    def decorator(self, *args, **kwargs):
        if os.environ.get("QUNIT_WATCH") == "1":
            kwargs["watch"] = True
        super(self, *args, **kwargs)

    return decorator


def start_tour(super):
    def decorator(*args, **kwargs):
        if os.environ.get("STEP_DELAY"):
            kwargs["step_delay"] = int(os.environ.get("STEP_DELAY"))
        super(*args, **kwargs)

    return decorator


class TestResult:
    def startTest(self, tests):
        pass


class ProgressTestResultPatch:
    def apply(self):
        if version == 1:
            super = unittest.result.TestResult
        elif version == 2:
            super = TestResult
        OdooTestResult.startTest = startTest(super.startTest)
        odoo.tests.HttpCase.browser_js = browser_js(odoo.tests.HttpCase.browser_js)
        odoo.tests.HttpCase.start_tour = start_tour(odoo.tests.HttpCase.start_tour)
