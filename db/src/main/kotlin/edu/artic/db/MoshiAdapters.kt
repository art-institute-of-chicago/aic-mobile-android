package edu.artic.db

import android.annotation.SuppressLint
import android.support.annotation.Nullable
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
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


fun Moshi.Builder.registerAdapters() = apply {
    add(KotlinJsonAdapterFactory())
    add(NullPrimitiveAdapter())
    add(ZonedDateTimeAdapter())
    add(FloorAdapter())
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

/**
 * Parses floor as an integer. We default to [INVALID_FLOOR] as 0 and
 * negative numbers are valid floors.
 *
 *
 * In the past, bugs related to this class have arisen (from Retrofit2) as
 *
 * `Unable to create converter for class edu.artic.db.models.ArticAppData for method AppDataApi.getBlob`
 *
 * or (from Moshi)
 *
 * `Non-null value 'floor' was null at $.some.path.`
 */
class FloorAdapter {

    @ToJson
    fun toText(@Floor floor: Int): String = floor.toString()

    /**
     * Due to a bug in Moshi 1.7.0, we require two overloads of this int converter.
     */
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @ToJson
    fun toTextBoxed(@Floor floor: java.lang.Integer): String = floor.toString()

    /**
     * Due to a bug in Moshi 1.7.0, we require two overloads of this int converter.
     *
     * Due to a bug in Kotlin 1.2.71, we must use the `java.lang.Integer` constructor.
     */
    @SuppressLint("UseValueOf")
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @FromJson
    @Floor
    fun fromTextBoxed(@Nullable text: String?): java.lang.Integer {
        return java.lang.Integer(fromText(text))
    }

    @FromJson
    @Floor
    fun fromText(@Nullable text: String?): Int {
        return if (text != null) {
            return try {
                text.toInt()
            } catch (e: NumberFormatException) {
                /** "LL" stands for 'Lower level' **/
                if (text == "LL") {
                    0
                } else {
                    INVALID_FLOOR
                }
            }
        } else {
            INVALID_FLOOR
        }

    }
}

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class Floor
