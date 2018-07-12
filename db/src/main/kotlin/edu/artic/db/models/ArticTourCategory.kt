package edu.artic.db.models

import com.squareup.moshi.Json

data class ArticTourCategory(
        @Json(name = "category") val category: String,
        @Json(name = "translations") val translations: List<Translation>) {

    data class Translation(
            @Json(name = "language") val language: String,
            @Json(name = "category") val category: String
    )
}