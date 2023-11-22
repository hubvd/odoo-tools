import logging
import os
import re
import subprocess
from dataclasses import dataclass
from enum import Enum
from rich.markup import escape

Result = Enum("Result", ["CANCELLED", "HANDLED"])


def loading(record):
    if record.name != "odoo.modules.loading":
        return

    if record.msg == "Modules loaded.":
        record.msg = "[bold green underline]All modules loaded ![/]"
        record.markup = True
        record.highlighter = None
        return Result.HANDLED


def server(record):
    if (
        record.name == "odoo.service.server"
        and record.msg == "HTTP service (werkzeug) running on %s:%s"
    ):
        if record.args[0] == "localhost":
            record.msg = "HTTP service running on http://%s:%s"
        else:
            record.msg = "HTTP service running on %s:%s"
        return Result.HANDLED


@dataclass
class LogReplacement:
    regex: str
    format: str
    markup: bool = True
    highlighter: bool = None
    level: int = None

    def __post_init__(self):
        self.regex = re.compile(self.regex)


replacements = [
    LogReplacement(
        r"^\"(?P<name>.*)\" passed (?P<count>\d+) tests\.$",
        "[test]{name}[/] passed [green]{count}[/] tests",
    ),
    LogReplacement(
        r"^Tour (?P<tour>.*): step '(?:Click here to go to the next step\. )?\(trigger: (?P<trigger>.*)\)' succeeded",
        "- [trigger]{trigger}",
    ),
    LogReplacement(
        r"^Tour (?P<tour>.*): step '(?P<description>.*?) ?\(trigger: (?P<trigger>.*)\)' succeeded",
        "- {description} [trigger]{trigger}",
    ),
    LogReplacement(r"^Running tour", None),
    LogReplacement(r"^Preparing tour", None),
    LogReplacement(r"^Tour .* failed at step .*", None, level=logging.ERROR),
    LogReplacement(r"^Owl is running in 'dev' mode", None),
    LogReplacement(r"^Views: using legacy view:", None),
    LogReplacement(r"^<html><head>", None),
    LogReplacement(
        r"^\[rpc\] response(?P<args>.*)",
        "[rpc] response {args}",
        markup=False,
        highlighter=True,
    ),
    LogReplacement(
        r"^Tour (?P<name>.*) on step: '?(?P<description>.*) \(trigger: (?P<trigger>.*?)\)'?$",
        "- {description} [trigger]{trigger}",
    ),
    LogReplacement(
        r"^Tour (?P<name>.*) on step: '?(?P<trigger>.*?)'?$", "- [trigger]{trigger}"
    ),
    LogReplacement("test successful", None),
]


def http_case(record):
    if record.msg == "Screenshot in: %s" and os.environ.get("TERM") == "xterm-kitty":
        subprocess.run(["kitty", "+kitten", "icat", record.args[0]])
        return Result.CANCELLED

    if record.msg == 'Evaluate test code "%s"' and record.args[0].startswith(
        "odoo.startTour("
    ):
        end = record.args[0].find("'", 16)
        tour = record.args[0][16:end]
        record.msg = f"Running tour [underline rgb(249,38,114)]%s[/]"
        record.args = (tour,)
        record.markup = True
        record.highlighter = None
        return Result.HANDLED
    elif 'code "%s"' in record.msg:
        record.msg = record.msg.replace('code "%s"', "code `%s`")
        return Result.HANDLED

    if not record.name.endswith("browser") or record.msg != "%s":
        return

    for replacement in replacements:
        if replacement.level and replacement.level != record.levelno:
            continue
        if match := replacement.regex.match(record.args[0]):
            if replacement.format is None:
                return Result.CANCELLED
            groups = match.groupdict()
            record.args = (
                replacement.format.format(**{k: escape(v) for k, v in groups.items()}),
            )
            record.markup = replacement.markup
            if not replacement.highlighter:
                record.highlighter = None
            return Result.HANDLED


filters = [
    lambda record: Result.CANCELLED if record.name == "odoo" else None,
    loading,
    server,
    http_case,
]
