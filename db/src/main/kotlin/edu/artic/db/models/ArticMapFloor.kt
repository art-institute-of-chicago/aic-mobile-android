package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
@JsonClass(generateAdapter = true)
data class ArticMapFloor(
        @PrimaryKey @Json(name = "label") val label: String,
        @Json(name = "floor_plan") val floorPlan: String?,
        @Json(name = "anchor_pixel_1") val anchorPixel1: String?,
        @Json(name = "anchor_pixel_2") val anchorPixel2: String?,
        @Json(name = "anchor_location_1") val anchorLocation1: String?,
        @Json(name = "anchor_location_2") val anchorLocation2: String?,
        @Json(name = "tile_images") val tiles: String
)