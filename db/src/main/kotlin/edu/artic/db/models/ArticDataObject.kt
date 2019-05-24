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
// FIXME: Way too easy to confuse with ArticObject. Also, can we drop the Artic prefix?
data class ArticDataObject(
        @Json(name = "image_server_url") val imageServerUrl: String,
        @Json(name = "data_api_url") val dataApiUrl: String,
        @Json(name = "exhibitions_endpoint") val exhibitionsEndpoint: String,
        @Json(name = "artworks_endpoint") val artworksEndpoint: String?,
        @Json(name = "galleries_endpoint") val galleriesEndpoint: String?,
        @Json(name = "images_endpoint") val imagesEndpoint: String?,
        @Json(name = "events_endpoint_v2") val eventsEndpoint: String?, //App is using v2 of this endpoint
        @Json(name = "autocomplete_endpoint") val autocompleteEndpoint: String?,
        @Json(name = "tours_endpoint") val toursEndpoint: String?,
        @Json(name = "multisearch_endpoint") val multiSearchEndpoint: String?,
        @Json(name = "membership_url") val membershipUrl: String?,
        @Json(name = "website_url") val websiteUrl: String?,
        @Json(name = "tickets_url") val ticketsUrl: String?,
        @PrimaryKey val id: Int = 0
) {

    companion object {
        var IMAGE_SERVER_URL = ""
    }

    val membershipUrlAndroid: String
        get() {
            return membershipUrl
                    .orEmpty()
                    .replace("utm_source=iphone", "utm_source=android")
        }

    val websiteUrlAndroid: String
        get() {
            return websiteUrl
                    .orEmpty()
                    .replace("utm_source=iphone", "utm_source=android")
        }

    val ticketsUrlAndroid: String
        get() {
            return ticketsUrl
                    .orEmpty()
                    .replace("utm_source=iphone", "utm_source=android")
        }
}