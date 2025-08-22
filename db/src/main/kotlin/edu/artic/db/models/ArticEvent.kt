package edu.artic.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.db.BuildConfig
import edu.artic.localization.util.toCurrentTimeZone
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticEvent(
        @Json(name = "short_description") val short_description: String?,
        @Deprecated("This valud is used in the current event api and will be replaced by `image_url`")
        @Json(name = "image") val image: String?,
        @Json(name = "image_url") val imageUrl: String?,
        @Json(name = "end_at") val end_at: ZonedDateTime,
        @Json(name = "button_url") val button_url: String?,
        @Json(name = "description") val description: String?,
        @Json(name = "location") val location: String?,
        @Json(name = "id") @PrimaryKey val id: String,
        @Json(name = "button_text") val button_text: String?,
        @Json(name = "title") val title: String,
        @Json(name = "start_at") val start_at: ZonedDateTime,
        @Json(name = "is_ticketed") val isTicketed: Boolean,
        @Json(name = "is_sales_button_hidden") val isSalesButtonHidden: Boolean?,
        @Json(name = "on_sale_at") val onSaleAt: ZonedDateTime?,
        @Json(name = "off_sale_at") val offSaleAt: ZonedDateTime?,
        @Json(name = "button_caption") val buttonCaption: String?
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
            return button_url ?: BuildConfig.DEFAULT_BUY_URL //Default URL for purchases
        }
}
