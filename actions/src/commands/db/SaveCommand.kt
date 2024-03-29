package com.github.hubvd.odootools.actions.commands.db

import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.actions.utils.DbManager
import com.github.hubvd.odootools.actions.utils.NotificationService
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.workspace.Workspaces

class SaveCommand(
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
    override fun fromTo(database: String) = database to database + "__SAVEPOINT"

    override fun notificationMessage(database: String) = "Saved db $database"
}
