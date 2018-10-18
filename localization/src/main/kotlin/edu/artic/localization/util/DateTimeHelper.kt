package edu.artic.localization.util

import edu.artic.localization.SPANISH
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.*
import org.threeten.bp.temporal.ChronoField.*
import java.util.Locale

/**
 * Helper class for determining how dates should be parsed from the API and/or displayed on screen.
 *
 * @author Sameer Dhakal (Fuzz)
 */

class DateTimeHelper {

    /**
     * The intended use of a given [DateTimeFormatter].
     */
    sealed class Purpose {

        object MonthThenDay : Purpose() {
            override fun obtainFormatter(locale: Locale): DateTimeFormatter {
                return when (locale) {
                    Locale.ENGLISH -> MONTH_DAY_FORMATTER
                    SPANISH -> DAY_MONTH_FORMATTER
                    else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                }.withLocale(locale)
            }
        }
        object HomeExhibition : Purpose() {
            override fun obtainFormatter(locale: Locale): DateTimeFormatter {
                return when (locale) {
                    Locale.ENGLISH -> HOME_EXHIBITION_DATE_FORMATTER
                    Locale.CHINESE -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                }.withLocale(locale)
            }
        }
        object HomeEvent : Purpose() {
            override fun obtainFormatter(locale: Locale): DateTimeFormatter {
                val timeFormat = when (locale) {
                    Locale.ENGLISH -> HOME_EVENT_TIME_AM_PM_POSTFIX_FORMATTER
                    Locale.CHINESE -> HOME_EVENT_TIME_AM_PM_PREFIX_FORMATTER
                    SPANISH -> HOME_EVENT_TIME_AM_PM_POSTFIX_FORMATTER
                    else -> DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
                }
                val dateFormat = MonthThenDay.obtainFormatter(locale)

                return DateTimeFormatterBuilder()
                        .append(dateFormat)
                        .appendLiteral("    ")
                        .append(timeFormat)
                        .toFormatter(locale)
            }
        }

        /**
         * Obtain the best [DateTimeFormatter] for the given purpose in the given locals.
         *
         * Different languages have different conventions for displaying this data.
         */
        abstract fun obtainFormatter(locale: Locale): DateTimeFormatter
    }

    companion object {


        private val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendText(MONTH_OF_YEAR, TextStyle.FULL)
                .appendLiteral(' ')
                .appendValue(DAY_OF_MONTH)
                .toFormatter()

        private val DAY_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendValue(DAY_OF_MONTH)
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, TextStyle.FULL)
                .toFormatter()

        /**
         * Preferred month-day-year formatter for [Locale.US].
         */
        private val HOME_EXHIBITION_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendText(MONTH_OF_YEAR, TextStyle.FULL)
                .appendLiteral(' ')
                .appendValue(DAY_OF_MONTH)
                .appendLiteral(", ")
                .appendValue(YEAR, 4)
                .toFormatter()

        private val HOME_EVENT_TIME_ONLY_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendValue(CLOCK_HOUR_OF_AMPM, 1, 2, SignStyle.NORMAL)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .toFormatter()

        /**
         * Preferred time-formatter for [Locale.ENGLISH] (among others).
         */
        private val HOME_EVENT_TIME_AM_PM_POSTFIX_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .append(HOME_EVENT_TIME_ONLY_FORMATTER)
                .appendLiteral(' ')
                .appendText(AMPM_OF_DAY)
                .toFormatter()

        /**
         * Preferred time-formatter for [Locale.CHINESE] (among others).
         */
        private val HOME_EVENT_TIME_AM_PM_PREFIX_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendText(AMPM_OF_DAY)
                .append(HOME_EVENT_TIME_ONLY_FORMATTER)
                .toFormatter()

    }
}

/**
 * Returns ZonedDateTime in current TimeZone.
 */
fun ZonedDateTime.toCurrentTimeZone(): ZonedDateTime {
    return this.withZoneSameInstant(ZoneId.systemDefault())
}
