#!/usr/bin/env python3

import os
import sys

sys.path.append(os.environ.get("ODOO_WORKSPACE") + "/odoo")

from patches import richlogger
from patches import progress
from patches import tests
from patches import webtests

patches = [
    richlogger.RichLogger(),
    progress.ModuleInstallProgress(),
    tests.ProgressTestResultPatch(),
    webtests.QunitLogger(),
]

for patch in patches:
    patch.apply()

# set server timezone in UTC before time module imported
__import__('os').environ['TZ'] = 'UTC'
import odoo

if __name__ == "__main__":
    odoo.cli.main()
