package edu.artic.db.models

import com.squareup.moshi.Json

data class ArticExhibition(
        @Json(name="title") val title: String?,
        @Json(name="image_filename") val imageFilename: String?,
        @Json(name="image_url") val imageUrl: String?,
        @Json(name="image_filemime") val imageFileMime: String?,
        @Json(name="image_filesize") val imageFileSize: String?,
        @Json(name="image_width") val imageWidth: String?,
        @Json(name="image_height") val imageHeight: String?,
        @Json(name="exhibition_id") val exhibitionId: String,
        @Json(name="sort") val sort: Int
)