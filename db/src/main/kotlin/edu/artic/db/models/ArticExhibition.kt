package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import org.threeten.bp.LocalDateTime

@Entity
data class ArticExhibition(
        @Json(name = "short_description") val short_description: String?,
        @Json(name = "aic_start_at") val aic_start_at: LocalDateTime,
        @Json(name = "legacy_image_mobile_url") val legacy_image_mobile_url: String?,
        @Json(name = "legacy_image_desktop_url") val legacy_image_desktop_url: String?,
        @Json(name = "web_url") val web_url: String?,
        @Json(name = "gallery_id") val gallery_id: String?,
        @Json(name = "id") @PrimaryKey val id: Int,
        @Json(name = "aic_end_at") val aic_end_at: LocalDateTime,
        @Json(name = "title") val title: String,
        var order: Int = -1
)