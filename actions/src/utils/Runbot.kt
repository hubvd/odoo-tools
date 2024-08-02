package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.actions.Secret
import com.github.hubvd.odootools.odoo.client.OdooClient
import com.github.hubvd.odootools.odoo.client.OdooCredential
import com.github.hubvd.odootools.odoo.client.core.ModelReference
import com.github.hubvd.odootools.odoo.client.searchRead
import kotlinx.serialization.Serializable
import org.kodein.di.*

val RUNBOT_MODULE by DI.Module {
    bind<OdooCredential>(tag = "runbot") {
        singleton {
            OdooCredential.SessionCredential(instance<Secret>(tag = "runbot_session").value)
        }
    }

    bind(tag = "runbot") {
        singleton {
            OdooClient(
                credential = instance(tag = "runbot"),
                host = "https://runbot.odoo.com",
                client = instance(),
            )
        }
    }

    bind<Runbot> { singleton { RunbotImpl(instance(tag = "runbot")) } }
}

interface Runbot {
    fun batches(base: String): List<ResolvedBatch>
}

class RunbotImpl(private val odoo: OdooClient) : Runbot {
    override fun batches(base: String): List<ResolvedBatch> {
        if (base == "master") TODO("bisect for master")

        val bundles = odoo.searchRead<Bundle>("runbot.bundle") {
            ("sticky" eq true) and ("project_id" eq 1)
        }

        val bundleIdx = bundles.indexOfFirst { it.name == base }
        val bundle = bundles[bundleIdx]
        val previousBundle = bundles[bundleIdx - 1]
        if (previousBundle.name == "master") {
            TODO("bisect for first sticky version")
        }
        val masterBundle = bundles.first { it.name == "master" }

        val batches = odoo.searchRead<Batch>("runbot.batch", order = "id desc") {
            "category_id" eq "Default" and (
                "bundle_id" eq bundle.id or (
                    "bundle_id" eq masterBundle.id and
                        ("create_date" ge previousBundle.createDate) and
                        ("create_date" le bundle.createDate)
                    )
                )
        }

        val commits = odoo.searchRead<RunbotCommit>("runbot.commit") {
            ("id" `in` batches.flatMap { it.commitIds }.toSet()) and ("repo_id" `in` arrayOf("odoo", "enterprise"))
        }

        // TODO: implement readGroup instead of grouping clientside..
        val (odooCommitsById, enterpriseCommitsById) = commits.partition {
            it.repoId.name == "odoo"
        }.let { (odoo, enterprise) ->
            odoo.associateByTo(HashMap()) { it.id } to enterprise.associateByTo(HashMap()) { it.id }
        }
        return batches.mapNotNull { batch ->
            val odoo = batch.commitIds.firstNotNullOfOrNull { odooCommitsById[it] }
                ?: return@mapNotNull null
            val enterprise = batch.commitIds.firstNotNullOfOrNull { enterpriseCommitsById[it] }
                ?: return@mapNotNull null
            ResolvedBatch(
                odoo = odoo.name,
                enterprise = enterprise.name,
            )
        }.toSet().toList()
    }
}

data class ResolvedBatch(val odoo: String, val enterprise: String)

@Serializable
private data class Bundle(val id: Long, val name: String, val createDate: String)

@Serializable
private data class Batch(val id: Long, val commitIds: List<Long>)

@Serializable
private data class RunbotCommit(val id: Long, val name: String, val repoId: ModelReference)
