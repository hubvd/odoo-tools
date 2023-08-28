package com.github.hubvd.odootools.odoo.client.core

import com.github.hubvd.odootools.odoo.client.serializers.ModelReferenceSerializer
import kotlinx.serialization.Serializable

@Serializable(ModelReferenceSerializer::class)
data class ModelReference(val id: Long, val name: String)
