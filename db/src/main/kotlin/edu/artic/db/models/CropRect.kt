package edu.artic.db.models

import com.squareup.moshi.Json

data class CropRect(
        @Json(name = "x") val x: String,
        @Json(name = "y") val y: String,
        @Json(name = "width") val width: String,
        @Json(name = "height") val height: String
)