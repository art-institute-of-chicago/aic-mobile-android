package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class ArticGallery(
        @Json(name = "title" ) val title: String?,
        @Json(name = "status" ) val status: String?,
        @Json(name = "nid" ) @PrimaryKey val nid: String,
        @Json(name = "type" ) val type: String?,
        @Json(name = "location" ) val location: String?,
        @Json(name = "latitude" ) val latitude: Double,
        @Json(name = "longitude" ) val longitude: Double,
        @Json(name = "floor" ) val floor: String?,
        @Json(name = "title_t" ) val titleT: String?,
        @Json(name = "gallery_id" ) val galleryId: String?,
        @Json(name = "is_boosted" ) val isBoosted: Boolean,
        @Json(name = "thumbnail" ) val thumbnail: String?,
        @Json(name = "closed" ) val closed: Boolean,
        @Json(name = "number" ) val number: String?,
        @Json(name = "category_titles" ) val categoryTitles: List<String>
) {
    /**
     * Returns [floor], parsed to an integer. We default to [Int.MIN_VALUE] as 0 is a valid floor.
     */
    val floorAsInt: Int
        get() = floor?.toIntOrNull() ?: Int.MIN_VALUE
}