package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import edu.artic.localization.util.toCurrentTimeZone
import edu.artic.ui.util.asCDNUri
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

@Entity
@Parcelize
data class ArticExhibition(
        @Json(name = "short_description") val short_description: String?,
        @Json(name = "aic_start_at") val aic_start_at: ZonedDateTime,
        @Json(name = "image_url") var imageUrl: String?,
        @Json(name = "web_url") val web_url: String?,
        @Json(name = "gallery_id") val gallery_id: String?,
        @Json(name = "id") @PrimaryKey val id: Int,
        @Json(name = "aic_end_at") val aic_end_at: ZonedDateTime,
        @Json(name = "title") val title: String,
        var order: Int = -1,

        /**
         * This value is defined by the associated [ArticGallery], associated
         * by [gallery_id].
         */
        var latitude: Double? = null,
        /**
         * This value is defined by the associated [ArticGallery], associated
         * by [gallery_id].
         */
        var longitude: Double? = null,
        /**
         * This value is defined by the associated [ArticGallery], associated
         * by [gallery_id].
         */
        var floor: Int? = null
) : Parcelable {

    val startTime: ZonedDateTime
        get() {
            return aic_start_at.toCurrentTimeZone()
        }

    val thumbUrl: String?
        get() {
            return imageUrl?.let {
                "$imageUrl?w=200&h=150"
            }
        }

    val endTime: ZonedDateTime
        get() {
            return aic_end_at.toCurrentTimeZone()
        }

}