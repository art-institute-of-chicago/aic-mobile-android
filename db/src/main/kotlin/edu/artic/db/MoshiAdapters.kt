package edu.artic.db

import android.support.annotation.Nullable
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatter.ISO_DATE_TIME


inline fun getMoshi(configureBlock: (Moshi.Builder.() -> Unit) = {}): Moshi =
        Moshi.Builder()
                .registerAdapters()
                .apply { configureBlock(this) }
                .build()

/**
 * Used to explicitly reference without needing to inject it into [SCError] for convenience.
 */
internal val apiErrorMoshi: Moshi = getMoshi()

fun Moshi.Builder.registerAdapters() = apply {
    add(KotlinJsonAdapterFactory())
    add(NullPrimitiveAdapter())
    add(ZonedDateTimeAdapter())
}

internal class NullPrimitiveAdapter {

    @FromJson
    fun intFromJson(@Nullable value: Int?): Int {
        return value ?: 0
    }

    @FromJson
    fun booleanFromJson(@Nullable value: Boolean?): Boolean {
        return value ?: false
    }

    @FromJson
    fun doubleFromJson(@Nullable value: Double?): Double {
        return value ?: 0.0
    }

    @FromJson
    fun fromJson(@Nullable jsonLong: Long?): Long { // Returns non-null default value.
        return jsonLong ?: 0L
    }
}

/**
 * Api returns date in UTC format, so we use UTC Zone across the application.
 *
 */
class ZonedDateTimeAdapter {

    var formatter = DateTimeFormatter.ISO_DATE_TIME

    @ToJson
    fun toText(dateTime: ZonedDateTime): String = dateTime.format(ISO_DATE_TIME)

    @FromJson
    fun fromText(text: String): ZonedDateTime = ZonedDateTime.parse(text, formatter)
}