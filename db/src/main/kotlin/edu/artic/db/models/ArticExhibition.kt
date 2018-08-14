package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

@Entity
@Parcelize
data class ArticExhibition(
        @Json(name = "short_description") val short_description: String?,
        @Json(name = "aic_start_at") val aic_start_at: ZonedDateTime,
        @Json(name = "legacy_image_mobile_url") var legacy_image_mobile_url: String?,
        @Json(name = "legacy_image_desktop_url") val legacy_image_desktop_url: String?,
        @Json(name = "web_url") val web_url: String?,
        @Json(name = "gallery_id") val gallery_id: String?,
        @Json(name = "id") @PrimaryKey val id: Int,
        @Json(name = "aic_end_at") val aic_end_at: ZonedDateTime,
        @Json(name = "title") val title: String,
        var order: Int = -1
) : Parcelable