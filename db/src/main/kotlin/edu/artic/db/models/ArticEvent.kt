package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class ArticEvent(
        @Json(name = "short_description") val short_description: String?,
        @Json(name = "image") val image: String?,
        @Json(name = "end_at") val end_at: String,
        @Json(name = "button_url") val button_url: String?,
        @Json(name = "description") val description: String?,
        @Json(name = "location") val location: String?,
        @Json(name = "id") @PrimaryKey val id: Int,
        @Json(name = "button_text") val button_text: String?,
        @Json(name = "title") val title: String,
        @Json(name = "start_at") val start_at: String

)