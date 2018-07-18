package edu.artic.db.models

import com.squareup.moshi.Json

/**
 * @author Sameer Dhakal (Fuzz)
 */

data class ArticEvent(
        @Json(name = "id") val eventId: Int,
        @Json(name = "title") val title: String,
        @Json(name = "short_description") val shortDescription: String?,
        @Json(name = "description") val longDescription: String?,
        @Json(name = "image") val imageUrl: String?,
        @Json(name = "location") val locationText: String?,
        @Json(name = "start_at") val startDate: String,
        @Json(name = "end_at") val endDate: String?,
        @Json(name = "button_text") val buttonText: String?)
