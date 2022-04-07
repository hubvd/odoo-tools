from odoo.http import Controller, route, Response


class DebugController(Controller):
    is_enabled = False

    @route("/debug", auth="none")
    def debug(self):
        if not self.is_enabled:
            try:
                import pydevd_pycharm
                pydevd_pycharm.settrace('localhost', port=10000, stdoutToServer=True, stderrToServer=True, suspend=False)
                self.is_enabled = True
                return Response(response='ok')
            except ImportError:
                return Response(status=500, response='pydevd_pycharm not installed\n run pip install pydevd-pycharm~=213.7172.26')
        else:
            return Response(response='already enabled')
