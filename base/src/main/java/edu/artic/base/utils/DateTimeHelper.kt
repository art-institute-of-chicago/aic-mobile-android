package edu.artic.base.utils

import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder

/**
@author Sameer Dhakal (Fuzz)
 */

class DateTimeHelper {
    companion object {
        const val DEFAULT_FORMAT: String = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"

        val DEFAULT_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendPattern(DEFAULT_FORMAT)
                .toFormatter()

        val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendPattern("MMMM d")
                .toFormatter()

        val HOME_EXHIBITION_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendPattern("MMMM d, yyyy")
                .toFormatter()

        val HOME_EVENT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendPattern("MMMM d   h:mm a")
                .toFormatter()
    }
}