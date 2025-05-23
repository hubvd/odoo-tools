from rich.progress import (
    Progress,
    SpinnerColumn,
    TextColumn,
    BarColumn,
    TaskProgressColumn,
)

from .console import console
from .patch_tools import patch_arguments

import odoo.modules.loading


class WrappedGraph:
    used = False
    id = 0

    def __init__(self, graph):
        self.parent = graph
        self.graph = list(graph)
        self.first = True
        self.idx = 0
        self.id = id
        self.has_progress = False

    def __iter__(self):
        WrappedGraph.id = WrappedGraph.id + 1
        new = WrappedGraph(self.graph)
        new.first = self.first
        if not new.first and len(new.graph) > 1:
            new.has_progress = True
            new.progress = Progress(
                SpinnerColumn(),
                TextColumn(
                    "[progress.description]{task.description} ({task.completed} of {task.total})[/]"
                ),
                BarColumn(bar_width=None),
                TaskProgressColumn(),
                expand=True,
                console=console,
            )
            new.progress.start()
            new.task = new.progress.add_task("Loading modules", total=len(new.graph))

        self.first = False
        return new

    def __next__(self):
        if self.idx < len(self.graph):
            cur = self.graph[self.idx]
            self.idx = self.idx + 1
            if self.has_progress:
                self.progress.update(self.task, completed=self.idx)
            return cur
        else:
            if self.has_progress:
                self.progress.update(self.task, visible=False)
                self.progress.remove_task(self.task)
                old_line = self.progress.live.console.line
                self.progress.live.console.line = lambda: ...
                self.progress.stop()
                self.progress.live.console.line = old_line
            raise StopIteration

    def __len__(self):
        return len(self.graph)

    def __contains__(self, name: str) -> bool:
        return name in self.parent

    def __getitem__(self, name: str):
        return self.parent[name]

    def __str__(self) -> str:
        return str(self.parent)

    def __getattr__(self, name):
        return getattr(self.parent, name)


class ModuleInstallProgress:
    @staticmethod
    def load_module_graph(*args, **kwargs):
        original_graph = args[1]
        graph = WrappedGraph(original_graph)
        args = (args[0], graph, *args[2:])
        return args, kwargs

    def apply(self):
        patch_arguments(
            odoo.modules.loading, "load_module_graph", self.load_module_graph
        )
