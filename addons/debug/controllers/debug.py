import logging
import warnings
from odoo.http import Controller, route, Response

class DebugController(Controller):

    @route("/debug/loggers", auth="none", csrf=False, methods=['GET'])
    def loggers(self, logger=None, level=None):
        if logger and level:
            logging.getLogger(logger).setLevel(level)
            return ""
        res = []
        for name, logger in logging.root.manager.loggerDict.items():
            if isinstance(logger, logging.Logger):
                res.append(f"{name} {logging.getLevelName(logger.level)}")
        return "\n".join(res)

    @route("/debug/attach", auth="none")
    def attach(self, port='10000'):
        try:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                import pydevd
                pydevd.stoptrace()
                pydevd.settrace('localhost', port=int(port), stdoutToServer=True, stderrToServer=True, suspend=False)
            return Response(response='ok')
        except ImportError:
            return Response(status=500, response='pydevd_pycharm not installed\n run pip install pydevd-pycharm~=213.7172.26')
        except Exception:
            return Response(status=500, response='An error occurred, maybe the dev server isn\'t started')

    @route("/debug/detach", auth="none")
    def detach(self):
        try:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                import pydevd
                pydevd.stoptrace()
            return Response(response='ok')
        except ImportError:
            return Response(status=500, response='pydevd_pycharm not installed\n run pip install pydevd-pycharm~=213.7172.26')
        except Exception:
            return Response(status=500, response='An error occurred, maybe the dev server isn\'t started')
