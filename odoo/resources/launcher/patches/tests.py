import logging
import time
import unittest

try:
    version = 1
    from odoo.tests.runner import OdooTestResult
except ImportError:
    version = 2
    from odoo.tests.result import OdooTestResult
from odoo import sql_db


_logger = logging.getLogger("odoo.tests.runner")


def startTest(super):
    def decorator(self, test):
        _logger.info(
            f"Starting [test]{self.getDescription(test)}[/]",
            extra={"markup": True},
        )
        if version == 2:
            self.testsRun += 1
        super(self, test)
        self.time_start = time.time()
        self.queries_start = sql_db.sql_counter

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
