#!/usr/bin/env python3

import os
import sys

sys.path.append(os.environ.get("ODOO_WORKSPACE") + "/odoo")

from patches import richlogger
from patches import progress
from patches import webtests
from patches import minifier

suspend = os.environ.get("ODOO_DEBUG_SUSPEND") != "0"

if os.environ.get("ODOO_DEBUG") == "1":
    import pydevd

    pydevd.settrace(
        "localhost",
        port=10000,
        stdoutToServer=False,
        stderrToServer=False,
        suspend=suspend,
    )

patches = [
    richlogger.RichLogger(),
    progress.ModuleInstallProgress(),
    webtests.WebTests(),
    minifier.Minifier(),
]

for patch in patches:
    patch.apply()

# set server timezone in UTC before time module imported
__import__("os").environ["TZ"] = "UTC"
import odoo.cli

if __name__ == "__main__":
    odoo.cli.main()
