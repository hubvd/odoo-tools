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
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Serializable
private data class Leave(
    private val id: Long,
    val startDatetime: String,
    val stopDatetime: String,
)

class IsOff(private val odooClient: OdooClient) : CliktCommand() {
    private val username by argument().validate {
        require(it.length in 3..4) {
            "not a valid trigram"
        }
    }

    override fun run() {
        val firstDay = DayOfWeek.MONDAY // WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val now = LocalDateTime.now()
        val month = now.month
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val records = odooClient.searchRead<Leave>("hr.leave.report.calendar") {
            ("employee_id.name" like "($username)") and ("start_datetime" le "2024-09-30 22:00:00") and
                ("stop_datetime" ge "2024-08-31 22:00:00")
        }

        val slots = records.map {
            val start = LocalDateTime.parse(it.startDatetime, formatter).atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
            val end = LocalDateTime.parse(it.stopDatetime, formatter).atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
            start to end
        }

        val dates = slots.flatMap {
            val endDate = it.second
            var currentDate = it.first

            val dates = mutableListOf<ZonedDateTime>()
            while (!currentDate.isAfter(endDate)) {
                dates.add(currentDate)
                currentDate = currentDate.plusDays(1)
            }

            dates
        }.sorted()

        val offDays = dates
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
