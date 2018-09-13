package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.db.Floor

@JsonClass(generateAdapter = true)
@Entity
data class ArticGallery(
        @Json(name = "title") val title: String?,
        @Json(name = "status") val status: String?,
        @Json(name = "nid") @PrimaryKey val nid: String,
        @Json(name = "type") val type: String?,
        /**
         * If this 'location' field is null, [latitude] and [longitude] should not
         * be used.
         */
        @Json(name = "location") val location: String?,
        @Json(name = "latitude") val latitude: Double,
        @Json(name = "longitude") val longitude: Double,
        @Floor @Json(name = "floor") val floor: Int,
        /**
         * NB: as established by the iOS codebase, please use [title] instead of this field.
         *
         * In the general sense we expect the two fields to be equivalent and this one may
         * be removed from the data model in a future commit.
         */
        @Json(name = "title_t") val titleT: String?,
        @Json(name = "gallery_id") val galleryId: String?,
        @Json(name = "is_boosted") val isBoosted: Boolean,
        @Json(name = "thumbnail") val thumbnail: String?,
        @Json(name = "closed") val closed: Boolean,
        @Json(name = "number") val number: String?,
        @Json(name = "category_titles") val categoryTitles: List<String>
) {

    // matches iOS implementation
    @Ignore
    val displayTitle: String = title
            ?.replace("Gallery ", "")
            ?.replace("Galleries ", "").orEmpty()
}