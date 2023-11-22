from rich.console import Console
from rich.theme import Theme

odoo_theme = Theme({"trigger": "yellow", "test": "bold steel_blue1 underline"})
console = Console(theme=odoo_theme)
