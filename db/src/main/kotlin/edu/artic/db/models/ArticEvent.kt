package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.base.utils.toCurrentTimeZone
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticEvent(
        @Json(name = "short_description") val short_description: String?,
        @Json(name = "image") val image: String?,
        @Json(name = "image_url") val imageUrl: String?,
        @Json(name = "end_at") val end_at: ZonedDateTime,
        @Json(name = "button_url") val button_url: String?,
        @Json(name = "description") val description: String?,
        @Json(name = "location") val location: String?,
        @Json(name = "id") @PrimaryKey val id: Int,
        @Json(name = "button_text") val button_text: String?,
        @Json(name = "title") val title: String,
        @Json(name = "start_at") val start_at: ZonedDateTime
        //button_caption, event_id, is_private, short_description
) : Parcelable {
    val startTime: ZonedDateTime
        get() {
            return start_at.toCurrentTimeZone()
        }

    val endTime: ZonedDateTime
        get() {
            return end_at.toCurrentTimeZone()
        }

    val imageURL: String
        get() {
            return image ?: imageUrl.orEmpty()
        }

    val buttonURL: String
        get() {
            return button_url ?: "https://sales.artic.edu/" //Default URL for purchases
        }
}
