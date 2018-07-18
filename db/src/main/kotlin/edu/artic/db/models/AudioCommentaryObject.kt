package edu.artic.db.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AudioCommentaryObject(
        @Json(name = "object_selector_number") val objectSelectorNumber: String?,
        @Json(name = "audio") val audio: String?
)