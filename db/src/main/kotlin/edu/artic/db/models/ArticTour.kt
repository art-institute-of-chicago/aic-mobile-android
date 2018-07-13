package edu.artic.db.models

import com.squareup.moshi.Json

data class ArticTour(
        @Json(name = "title") val title: String,
        @Json(name = "image_url") val imageUrl: String,
        @Json(name = "description") val description: String,
        @Json(name = "tour_duration") val tourDuration: String,
        @Json(name = "tour_stops") val tourStops: List<TourStop>?
) {
    data class TourDate(
            @Json(name = "start_date") val startDate: String,
            @Json(name = "end_date") val endDate: String
    )

    data class TourStop(
            @Json(name = "object") val objectId: String,
            val audio_id: String,
            val audio_bumper: String?,
            val sort: Int
    )

    data class Translation(
            @Json(name = "language") val language: String,
            @Json(name = "title") val title: String,
            @Json(name = "description") val description: String,
            @Json(name = "description_html") val description_html: String,
            @Json(name = "intro") val intro: String,
            @Json(name = "intro_html") val intro_html: String,
            @Json(name = "tour_duration") val tour_duration: String
    )
}