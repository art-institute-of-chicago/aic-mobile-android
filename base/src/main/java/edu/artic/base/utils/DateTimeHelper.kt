package edu.artic.base.utils

import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.format.SignStyle
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.ChronoField.*

/**
@author Sameer Dhakal (Fuzz)
 */

class DateTimeHelper {
    companion object {

        val DEFAULT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

        val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendText(MONTH_OF_YEAR, TextStyle.FULL)
                .appendLiteral(' ')
                .appendValue(DAY_OF_MONTH)
                .toFormatter()

        val HOME_EXHIBITION_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendText(MONTH_OF_YEAR, TextStyle.FULL)
                .appendLiteral(' ')
                .appendValue(DAY_OF_MONTH)
                .appendLiteral(", ")
                .appendValue(YEAR, 4)
                .toFormatter()

        val HOME_EVENT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .append(MONTH_DAY_FORMATTER)
                .appendLiteral("   ")
                .appendValue(CLOCK_HOUR_OF_AMPM, 1, 2, SignStyle.NORMAL)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendLiteral(' ')
                .appendText(AMPM_OF_DAY)
                .toFormatter()
    }
}

/**
 * Returns ZonedDateTime in current TimeZone.
 */
fun ZonedDateTime.toCurrentTimeZone(): ZonedDateTime {
    return this.withZoneSameInstant(ZoneId.systemDefault())
}
