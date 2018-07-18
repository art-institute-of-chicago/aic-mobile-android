package edu.artic.db

import android.support.annotation.Nullable
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import edu.artic.base.utils.DateTimeHelper
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*


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
    add(LocalDateTimeAdapter())
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
 * Description:
 */
class LocalDateTimeAdapter {

    var formatter = DateTimeFormatter.ofPattern(
            "[${DateTimeHelper.DEFAULT_FORMAT}]" +
                    "[yyyy/MM/dd HH:mm:ss.SSSSSS]" +
                    "[yyyy-MM-dd HH:mm:ss[.SSS]]" +
                    "[ddMMMyyyy:HH:mm:ss.SSS[ Z]]"
    )

    @ToJson
    fun toText(dateTime: LocalDateTime): String = dateTime.format(formatter)

    @FromJson
    fun fromText(text: String): LocalDateTime = LocalDateTime.parse(text, formatter)
}