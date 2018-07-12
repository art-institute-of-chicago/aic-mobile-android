package edu.artic.db.models

import com.squareup.moshi.Json

data class AudioCommentaryObject(
        @Json(name = "object_selector_number") val objectSelectorNumber: String,
        @Json(name = "audio") val audio: String
)