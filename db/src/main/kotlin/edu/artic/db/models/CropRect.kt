package edu.artic.db.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class CropRect(
        @Json(name = "x") val x: String,
        @Json(name = "y") val y: String,
        @Json(name = "width") val width: String,
        @Json(name = "height") val height: String
) : Parcelable