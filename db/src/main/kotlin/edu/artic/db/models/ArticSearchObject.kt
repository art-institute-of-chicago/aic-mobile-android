package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticSearchObject(
        @Json(name = "search_strings") val searchStrings: Map<String, String>,
        /**
         * This (I think?) is a list of [ArticObject.nid] values.
         */
        @Json(name = "search_objects") val searchObjects: List<Int>
)


@Entity
data class ArticSearchSuggestionsObject(
        val searchStrings: List<String>,
        val searchObjects: List<String>,
        @PrimaryKey val id : Int = 0
)