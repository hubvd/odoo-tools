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

enabled_patches = os.environ.get("ODOO_PATCHES", "all").split(",")

patches = {
    "rich": richlogger.RichLogger(),
    "progress": progress.ModuleInstallProgress(),
    "tests": webtests.WebTests(),
    "minifier": minifier.Minifier(),
}

if "all" in enabled_patches:
    active_patches = patches.values()
else:
    active_patches = [patches[name] for name in enabled_patches if name in patches]

for patch in active_patches:
    patch.apply()

# set server timezone in UTC before time module imported
__import__("os").environ["TZ"] = "UTC"
import odoo.cli  # noqa:E402

if __name__ == "__main__":
    odoo.cli.main()
