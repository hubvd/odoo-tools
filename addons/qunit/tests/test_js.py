import os
import odoo.tests
import odoo.release


def qunit_error_checker(message):
    # We don't want to stop qunit if a qunit is breaking.

    # '%s/%s test failed.' case: end message when all tests are finished
    if 'tests failed.' in message:
        return True

    # "QUnit test failed" case: one qunit failed. don't stop in this case
    if "QUnit test failed:" in message:
        return False

    return True  # in other cases, always stop (missing dependency, ...)


@odoo.tests.tagged('post_install', '-at_install')
class WebSuite(odoo.tests.HttpCase):

    def test_qunit(self):
        url = "/web/tests?mod=web"

        if os.environ.get("QUNIT_FAILFAST", "1") == "1":
            url += "&failfast"

        filter = os.environ.get("QUNIT_FILTER", "!punjabi (gurmukhi) has the correct numbering system")
        url += f"&filter={filter}"

        if odoo.release.version_info[0] >= 16:
            self.browser_js(url, "", "", login='admin', timeout=1800, error_checker=qunit_error_checker)
        else:
            self.browser_js(url, "", "", login='admin', timeout=1800)
