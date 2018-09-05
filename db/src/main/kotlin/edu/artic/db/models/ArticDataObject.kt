package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Global configuration data for the app.
 */
@JsonClass(generateAdapter = true)
@Entity
data class ArticDataObject(
        @Json(name = "image_server_url") val imageServerUrl: String,
        @Json(name = "data_api_url") val dataApiUrl: String,
        @Json(name = "exhibitions_endpoint") val exhibitionsEndpoint: String,
        @Json(name = "artworks_endpoint") val artworksEndpoint: String?,
        @Json(name = "galleries_endpoint") val galleriesEndpoint: String?,
        @Json(name = "images_endpoint") val imagesEndpoint: String?,
        @Json(name = "events_endpoint") val eventsEndpoint: String?,
        @Json(name = "autocomplete_endpoint") val autocompleteEndpoint: String?,
        @Json(name = "tours_endpoint") val toursEndpoint: String?,
        @Json(name = "multisearch_endpoint") val multiSearchEndpoint: String?,
        @Json(name = "membership_url") val membershipUrl: String?,
        @Json(name = "website_url") val websiteUrl: String?,
        @Json(name = "tickets_url") val ticketsUrl: String?,
        @PrimaryKey val id : Int = 0
)