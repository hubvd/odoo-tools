package com.github.hubvd.odootools.worktree.commands

import org.kodein.di.DI
import org.kodein.di.bindSet
import org.kodein.di.new
import org.kodein.di.singleton

val COMMAND_MODULE = DI.Module("command") {
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
