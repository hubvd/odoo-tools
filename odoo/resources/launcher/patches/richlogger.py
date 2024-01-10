import logging
import logging.handlers
from logging import LogRecord
import threading
from datetime import datetime
from zoneinfo import ZoneInfo

import werkzeug.serving

from rich.highlighter import ReprHighlighter
from rich.logging import RichHandler
from rich.text import Span

import odoo.netsvc
from odoo.netsvc import DBFormatter, PerfFilter

from .logging_filters import Result, filters
from .console import console


class OdooRichHandler(RichHandler):
    def emit(self, record):
        for filter in filters:
            res = filter(record)
            if not res:
                continue
            elif res == Result.CANCELLED:
                return
            elif res == Result.HANDLED:
                super().emit(record)
                return
        super().emit(record)

    def render(self, *, record, traceback, message_renderable):
        path = record.name
        if path.startswith("odoo.addons."):
            path = "@" + path[12:]

        if path.endswith(".browser"):
            path = "browser"
        elif len(path) > 36:
            parts = path.split(".")
            if len(parts) > 1:
                path = parts[-1][:33] + "..."
            else:
                path = path[:33] + "..."

        level = self.get_level_text(record)
        time_format = None if self.formatter is None else self.formatter.datefmt
        log_time = datetime.fromtimestamp(record.created, ZoneInfo("Europe/Brussels"))
        log_renderable = self._log_render(
            self.console,
            [message_renderable] if not traceback else [message_renderable, traceback],
            log_time=log_time,
            time_format=time_format,
            level=level,
            path=path,
            line_no=record.lineno,
            link_path=record.pathname if self.enable_link_path else None,
        )
        return log_renderable

    def render_message(self, record: "LogRecord", message: str):
        message_text = super().render_message(record, message)
        dbname = getattr(threading.current_thread(), "dbname", "?")
        level_name = record.levelname
        message_text._spans.append(
            Span(0, len(dbname), f"logging.level.{level_name.lower()}")
        )
        return message_text


class DurationHighlighter(ReprHighlighter):
    highlights = ReprHighlighter.highlights + [
        r"(?P<number>(?<!\w)\-?[0-9]+\.?[0-9]*s)",
    ]


class RichLogger:
    @staticmethod
    def post_init_logger():
        logger = logging.getLogger()

        for handler in logger.handlers:
            logger.removeHandler(handler)
        for filter in logger.filters:
            logger.removeFilter(filter)

        format = "%(dbname)s: %(message)s %(perf_info)s"
        handler = OdooRichHandler(
            console=console,
            show_time=False,
            show_level=False,
            rich_tracebacks=True,
            highlighter=DurationHighlighter(),
        )
        formatter = DBFormatter(format, "%Y-%m-%d %H:%M:%S")
        handler.setFormatter(formatter)
        logger.addHandler(handler)
        logging.getLogger("werkzeug").addFilter(PerfFilter())

    def init_logger(original):
        def decorator(*args, **kwargs):
            original(*args, **kwargs)
            RichLogger.post_init_logger()

        return decorator

    def apply(self):
        werkzeug_logger = logging.getLogger("werkzeug")

        def log_request(self, code="-", size="-"):
            try:
                path = werkzeug.serving.uri_to_iri(self.path)
                msg = f"{self.command} {path}"
            except AttributeError:
                msg = self.requestline
                code = str(code)
            werkzeug_logger.info("%s %s %s", msg, code, size)

        werkzeug.serving.WSGIRequestHandler.log_request = log_request

        odoo.netsvc.init_logger = RichLogger.init_logger(odoo.netsvc.init_logger)
