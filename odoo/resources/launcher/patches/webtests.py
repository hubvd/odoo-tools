import logging
import re
import odoo.tests.common


class LogReplacement:
    def __init__(
        self, regex: str, format: str, markup: bool = True, highlighter: bool = None
    ):
        self.regex = re.compile(regex)
        self.format = format
        self.markup = markup
        self.highlighter = highlighter


class CustomAdapter(logging.LoggerAdapter):
    replacements = [
        LogReplacement(
            r"^\"(?P<name>.*)\" passed (?P<count>\d+) tests\.$",
            "[bold steel_blue1 underline]{name}[/] passed [green]{count}[/] tests",
        ),
        LogReplacement(
            r"^Tour (?P<tour>.*): step '(?:Click here to go to the next step\. )?\(trigger: (?P<step>.*)\)' succeeded",
            "Step [underline][bold steel_blue1]{step}[/bold steel_blue1][/underline] succeeded",
        ),
        LogReplacement(
            r"^Running tour (?P<tour>.*)",
            "Running tour [underline rgb(249,38,114)]{tour}[/]",
        ),
        LogReplacement(
            r"Tour (?P<tour>.*) failed at step (?P<step>.*)",
            "Tour {tour} failed at step [bold reverse red]{step}[/]",
        ),
        LogReplacement(r"^Owl is running in 'dev' mode", None),
        LogReplacement(r"^Views: using legacy view:", None),
        LogReplacement(r"^<html><head>", None),
        LogReplacement(
            r"^\[rpc\] response(?P<args>.*)",
            "[rpc] response {args}",
            markup=False,
            highlighter=True,
        ),
    ]

    def runbot(self, message, *args, **kws):
        self.log(25, message, *args, **kws)

    def log(self, *args, **kwargs):
        if len(args) < 3:
            return self.logger.log(*args, **kwargs)
        level, fmt, msg = args[:3]
        msg = str(msg)

        for replacement in self.replacements:
            if match := replacement.regex.match(msg):
                if replacement.format is None:
                    return
                groups = match.groupdict()
                msg = replacement.format.format(**groups)
                extra = kwargs.get("extra", {})
                extra["markup"] = replacement.markup
                if not replacement.highlighter:
                    extra["highlighter"] = None
                kwargs["extra"] = extra
                break

        args = [level, fmt, msg] + list(args[3:])
        return self.logger.log(*args, **kwargs)

    def getChild(self, *args, **kwargs):
        return CustomAdapter(self.logger.getChild(*args, **kwargs), {})


def init_chrome(super):
    def decorator(self, *args, **kwargs):
        super(self, *args, **kwargs)
        self._logger = CustomAdapter(self._logger, {})

    return decorator


def _spawn_chrome(super):
    def decorator(self, cmd):
        if "--remote-allow-origins=*" not in cmd:
            cmd.insert(len(cmd) - 1, "--remote-allow-origins=*")
        return super(self, cmd)

    return decorator


class QunitLogger:
    def apply(self):
        odoo.tests.common.ChromeBrowser.__init__ = init_chrome(
            odoo.tests.common.ChromeBrowser.__init__
        )

        odoo.tests.common.ChromeBrowser._spawn_chrome = _spawn_chrome(
            odoo.tests.common.ChromeBrowser._spawn_chrome
        )
