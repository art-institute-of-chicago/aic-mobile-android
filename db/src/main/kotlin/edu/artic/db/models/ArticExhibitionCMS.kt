package edu.artic.db.models

/**
@author Sameer Dhakal (Fuzz)
 */

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class ArticExhibitionCMS(
        @Json(name = "exhibition_id") @PrimaryKey val id: String,
        @Json(name = "sort") val sort: Int,
        @Json(name = "title") val title: String,
        @Json(name = "image_url") val imageUrl: String?
)