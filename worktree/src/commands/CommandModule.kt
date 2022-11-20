package com.github.hubvd.odootools.worktree.commands

import org.kodein.di.*

val commandModule = DI.Module("command") {
    bindSet {
        add { singleton { new(::AddCommand) } }
        add { singleton { new(::CurrentCommand) } }
        add { singleton { new(::DefaultCommand) } }
        add { singleton { new(::InitCommand) } }
        add { singleton { new(::ListCommand) } }
        add { singleton { new(::PruneCommand) } }
        add { singleton { new(::RebuildCommand) } }
        add { singleton { new(::RepositoryCommand) } }
    }
}
