package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.hubvd.odootools.odoo.client.OdooClient
import com.github.hubvd.odootools.odoo.client.core.ModelReference
import com.github.hubvd.odootools.odoo.client.searchRead
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable
private data class Leave(
    private val id: Long,
    val employeeId: ModelReference,
    @Serializable(UtcToSystemDefaultZonedDateTimeSerializer::class) val startDatetime: ZonedDateTime,
    @Serializable(UtcToSystemDefaultZonedDateTimeSerializer::class) val stopDatetime: ZonedDateTime,
)

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val formatter2 = DateTimeFormatter.ofPattern("dd")
private val utc = ZoneId.of("UTC")

private class UtcToSystemDefaultZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor(
        "UtcToSystemDefaultZonedDateTimeSerializer",
        PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime = LocalDateTime.parse(decoder.decodeString(), formatter)
        .atZone(utc)
        .withZoneSameInstant(ZoneId.systemDefault())
}

private val trigramRe = Regex("""^(.*) \(([a-zA-Z]{2,4})\)$""")

class IsOffCommand(private val odooClient: OdooClient) : CliktCommand() {
    private val usernames by argument().multiple().validate {
        it.forEach {
            require(it.length in 3..4) {
                "`$it` is not a valid trigram"
            }
        }
    }

    override fun run() {
        val now = LocalDateTime.now().withHour(3).withMinute(0)

        val start = now
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(utc)

        val end = start.plusDays(15) // TODO

        val offDays = odooClient
            .searchRead<Leave>("hr.leave.report.calendar") {
                val employees = usernames.map { "employee_id.name" `=ilike` "%($it)" }.reduce { a, b -> a or b }
                employees and
                    ("start_datetime" le formatter.format(end)) and
                    ("stop_datetime" ge formatter.format(start))
            }.groupBy { leave ->
                trigramRe.find(leave.employeeId.name)?.groupValues?.get(2)?.lowercase()!!
            }.mapValues { entry -> entry.value.map { it } }

        val days = generateSequence(now.atZone(ZoneId.systemDefault())) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .toList()

        val table = table {
            tableBorders = Borders.ALL
            header {
                row {
                    style(TextColors.magenta, bold = true)
                    cell("Employee")
                    days.forEach {
                        cell(it.format(formatter2)) {
                            if (it.dayOfWeek == DayOfWeek.MONDAY) {
                                style = TextColors.cyan
                            }
                        }
                    }
                }
            }
            body {
                cellBorders = Borders.LEFT_RIGHT
                rowStyles(TextColors.blue, TextColors.green)
                offDays.entries.forEach { (employee, entries) ->
                    row {
                        cell(employee)
                        for (day in days) {
                            var presentMorning = true
                            var presentAfternoon = true

                            val morning = day.withHour(11).withMinute(0)!!
                            val afternoon = day.withHour(14).withMinute(0)!!

                            for (entry in entries) {
                                if (presentMorning && entry.startDatetime <= morning && morning <= entry.stopDatetime) {
                                    presentMorning = false
                                }

                                if (presentAfternoon &&
                                    entry.startDatetime <= afternoon &&
                                    afternoon <= entry.stopDatetime
                                ) {
                                    presentAfternoon = false
                                }

                                if (!presentAfternoon && !presentMorning) break
                            }

                            val content = when {
                                !presentMorning && !presentAfternoon -> ENTIRE_DAY
                                !presentMorning -> MORNING
                                !presentAfternoon -> AFTERNOON
                                else -> ""
                            }
                            cell(content)
                        }
                    }
                }
            }
        }

        terminal.println(table)
    }
}

val MORNING = "\uD83C\uDF05"
val AFTERNOON = "\uD83C\uDF06"
val ENTIRE_DAY = "\uD83E\uDDA5"
