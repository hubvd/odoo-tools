from odoo.http import Controller, route, Response

class DebugController(Controller):

    @route("/debug/attach", auth="none")
    def attach(self, port='10000'):
        try:
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
            import pydevd
            pydevd.stoptrace()
            return Response(response='ok')
        except ImportError:
            return Response(status=500, response='pydevd_pycharm not installed\n run pip install pydevd-pycharm~=213.7172.26')
        except Exception:
            return Response(status=500, response='An error occurred, maybe the dev server isn\'t started')
