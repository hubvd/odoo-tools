package com.github.hubvd.odootools.odoo.client

import com.github.hubvd.odootools.config.Config
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.multiton

val ODOO_CLIENT_MODULE by DI.Module {
    bind<OdooCredential> {
        multiton { name: String ->
            instance<Config>().get(
                "clients.$name",
                OdooCredentialSerializer,
            )
        }
    }

    bind<OdooClient> {
        multiton { name: String ->
            OdooClient(instance(arg = name), instance())
        }
    }
}
