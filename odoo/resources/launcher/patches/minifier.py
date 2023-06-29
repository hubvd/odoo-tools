import rjsmin
from odoo.addons.base.models import assetsbundle


class Minifier:
    def apply(self):
        assetsbundle.rjsmin = rjsmin.jsmin
