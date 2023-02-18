import logging
import re
import odoo.tests.common


class CustomAdapter(logging.LoggerAdapter):
    qunit_regex = re.compile(r"^\"(?P<name>.*)\" passed (?P<count>\d+) tests\.$")

    def runbot(self, message, *args, **kws):
        self.log(25, message, *args, **kws)

    def log(self, *args, **kwargs):
        if len(args) < 3:
            return self.logger.log(*args, **kwargs)
        level, fmt, msg = args[:3]
        msg = str(msg)
        if match := CustomAdapter.qunit_regex.match(msg):
            groups = match.groupdict()
            msg = f"[bold steel_blue1 underline]{groups['name']}[/] passed [green]{groups['count']}[/] tests."
            extra = kwargs.get("extra", {})
            extra["markup"] = True
            extra["highlighter"] = None
            kwargs["extra"] = extra
        args = [level, fmt, msg] + list(args[3:])
        return self.logger.log(*args, **kwargs)

    def getChild(self, *args, **kwargs):
        return CustomAdapter(self.logger.getChild(*args, **kwargs), {})


def init_chrome(super):
    def decorator(self, *args, **kwargs):
        super(self, *args, **kwargs)
        self._logger = CustomAdapter(self._logger, {})

    return decorator


class QunitLogger:
    def apply(self):
        odoo.tests.common.ChromeBrowser.__init__ = init_chrome(
            odoo.tests.common.ChromeBrowser.__init__
        )
