package com.github.hubvd.odootools.odoo.client

sealed class OdooCredential {
    class JsonRpcCredential(val database: String, val userId: Long, val apiKey: String) : OdooCredential()
    class SessionCredential(val session: String) : OdooCredential()
}
