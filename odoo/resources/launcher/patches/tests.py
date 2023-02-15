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
from odoo.tests.runner import OdooTestResult
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


class ProgressTestResultPatch:
    def apply(self):
        odoo.tests.runner.OdooTestResult.startTest = startTest(
            unittest.result.TestResult.startTest
        )
        odoo.tests.HttpCase.browser_js = browser_js(odoo.tests.HttpCase.browser_js)
