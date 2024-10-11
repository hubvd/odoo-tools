package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.grid
import com.github.hubvd.odootools.odoo.client.OdooClient
import com.github.hubvd.odootools.odoo.client.searchRead
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Serializable
private data class Leave(
    private val id: Long,
    @Serializable(UtcToSystemDefaultZonedDateTimeSerializer::class) val startDatetime: ZonedDateTime,
    @Serializable(UtcToSystemDefaultZonedDateTimeSerializer::class) val stopDatetime: ZonedDateTime,
)

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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

class IsOffCommand(private val odooClient: OdooClient) : CliktCommand() {
    private val username by argument().validate {
        require(it.length in 3..4) {
            "not a valid trigram"
        }
    }

    override fun run() {
        val firstDay = DayOfWeek.MONDAY // WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val now = LocalDateTime.now()
        val month = now.month
        val start = YearMonth.from(now)
            .plusMonths(-1)
            .atEndOfMonth()
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(utc)
        val end = YearMonth
            .from(now)
            .atEndOfMonth()
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(utc)

        val offDays = odooClient
            .searchRead<Leave>("hr.leave.report.calendar") {
                ("employee_id.name" like "($username)") and
                    ("start_datetime" le formatter.format(end)) and
                    ("stop_datetime" ge formatter.format(start))
            }
            .asSequence()
            .flatMap { record ->
                generateSequence<ZonedDateTime>(record.startDatetime) { it.plusDays(1) }
                    .takeWhile<ZonedDateTime> { !it.isAfter(record.stopDatetime) }
            }
            .filter { it.month == month }
            .map { it.dayOfMonth }
            .toHashSet()

        val grid = grid {
            row {
                cell(
                    month.getDisplayName(
                        TextStyle.FULL,
                        Locale.getDefault(),
                    ),
                ) {
                    columnSpan = 7
                    align = TextAlign.CENTER
                }
            }
            row {
                cellsFrom(
                    generateSequence<DayOfWeek>(firstDay) { it.plus(1) }.take(7).map {
                        it.getDisplayName(
                            TextStyle.SHORT_STANDALONE,
                            Locale.getDefault(),
                        )
                    }.asIterable(),
                )
            }

            val days = generateSequence(LocalDateTime.of(now.year, month, 1, 0, 0)) { it.plusDays(1) }
                .takeWhile { it.month == month }
                .toList()
                .reversed()
                .toMutableList()

            // TODO: chuncked

            var currentDay = firstDay
            var cells = ArrayList<String>(7)
            while (days.isNotEmpty()) {
                if (days.last().dayOfWeek == currentDay) {
                    val day = days.removeLast()
                    if (day.dayOfMonth in offDays) {
                        cells.add(TextColors.red(day.dayOfMonth.toString()))
                    } else {
                        cells.add(day.dayOfMonth.toString())
                    }
                } else {
                    cells.add("")
                }
                currentDay = currentDay.plus(1)
                if (currentDay == firstDay) {
                    rowFrom(cells) {
                        align = TextAlign.RIGHT
                    }
                    cells.clear()
                }
            }
            if (cells.isNotEmpty()) {
                rowFrom(cells) {
                    align = TextAlign.RIGHT
                }
            }
        }

        terminal.println(grid)
    }
}
