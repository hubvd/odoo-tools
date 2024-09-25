package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.Companion.hyperlink
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.hubvd.odootools.actions.ActionsConfig
import com.github.hubvd.odootools.odoo.client.OdooClient
import com.github.hubvd.odootools.odoo.client.searchRead
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
private data class Activity(
    val id: Long,
    val name: String,
    @SerialName("my_activity_date_deadline")
    @Serializable(LocalDateSerializer::class)
    val deadline: LocalDate,
)

class LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    override fun serialize(encoder: Encoder, value: LocalDate) = throw UnsupportedOperationException()
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString(), formatter)
}

class ActivitiesCommand(private val odooClient: OdooClient, private val config: ActionsConfig) : CliktCommand() {
    override fun run() {
        val now = LocalDate.now()
        val records = odooClient.searchRead<Activity>("project.task") {
            ("activity_user_id.name" like config.trigram)
        }.sortedBy(Activity::deadline)
        records.forEach { activity ->
            val color = when {
                activity.deadline.isBefore(now) -> yellow
                activity.deadline == now -> green
                else -> cyan
            }
            val url = hyperlink("https://www.odoo.com/odoo/project/49/tasks/${activity.id}")(underline(activity.name))
            terminal.println("${color(activity.deadline.toString())} $url")
        }
    }
}
