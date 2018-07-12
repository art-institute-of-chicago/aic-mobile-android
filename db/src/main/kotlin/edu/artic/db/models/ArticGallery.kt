package edu.artic.db.models

import com.squareup.moshi.Json

data class ArticGallery(
        @Json(name = "title" ) val title: String?,
        @Json(name = "status" ) val status: String?,
        @Json(name = "nid" ) val nid: String?,
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
)