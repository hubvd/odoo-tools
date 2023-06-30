from _pydevd_bundle.pydevd_extension_api import StrPresentationProvider
from lxml import etree


class LxmlProvider(StrPresentationProvider):
    def can_provide(self, type_object, type_name):
        return issubclass(type_object, etree._Element)

    def get_str(self, val):
        return etree.tostring(val, encoding="utf-8").decode("utf-8")
