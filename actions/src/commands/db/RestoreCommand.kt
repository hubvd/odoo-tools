package com.github.hubvd.odootools.actions.commands.db

import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.actions.utils.DbManager
import com.github.hubvd.odootools.actions.utils.NotificationService
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.workspace.Workspaces

class RestoreCommand(
    dbManager: DbManager,
    odooctl: Odooctl,
    kitty: Kitty,
    notificationService: NotificationService,
    workspaces: Workspaces,
) : BaseCopyCommand(
    dbManager,
    odooctl,
    kitty,
    notificationService,
    workspaces,
) {
    override fun fromTo(database: String) = database + "__SAVEPOINT" to database

    override fun notificationMessage(database: String) = "Restored db $database"
}
