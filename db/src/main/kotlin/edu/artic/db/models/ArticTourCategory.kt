package edu.artic.db.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.localization.BaseTranslation

@JsonClass(generateAdapter = true)
data class ArticTourCategory(
        @Json(name = "category") val category: String,
        @Json(name = "translations") val translations: List<Translation>) {

    @JsonClass(generateAdapter = true)
    data class Translation(
            @Json(name = "language") val language: String,
            @Json(name = "category") val category: String
    ) : BaseTranslation
}