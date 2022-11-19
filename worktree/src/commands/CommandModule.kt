package com.github.hubvd.odootools.worktree.commands

import org.kodein.di.DI
import org.kodein.di.bindSet
import org.kodein.di.instance
import org.kodein.di.singleton

val commandModule = DI.Module("command") {
    bindSet {
        add { singleton { AddCommand(instance(), instance(), instance(), instance()) } }
        add { singleton { CurrentCommand(instance(), instance()) } }
        add { singleton { DefaultCommand(instance(), instance()) } }
        add { singleton { InitCommand() } }
        add { singleton { ListCommand(instance(), instance()) } }
        add { singleton { PruneCommand(instance(), instance()) } }
        add { singleton { RebuildCommand(instance(), instance(), instance()) } }
        add { singleton { RepositoryCommand(instance()) } }
    }
}
