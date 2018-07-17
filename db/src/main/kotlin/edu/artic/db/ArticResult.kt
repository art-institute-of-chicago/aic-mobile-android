package edu.artic.db

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class ArticResult<T>(
        @Json(name = "pagination") val pagination : ResultPagination,
        @Json(name = "data") val data : List<T>
)

data class ResultPagination(
        var total: Int,
        var limit: Int,
        var offset: Int,
        var total_pages: Int,
        var current_page: Int
)