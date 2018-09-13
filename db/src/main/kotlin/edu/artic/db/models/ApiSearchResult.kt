package edu.artic.db.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.db.models.ApiSearchContent.SearchedTour

/**
 * While [ArticAppData] only contains content that is currently on view, search
 * results might bring up any artwork. The same applies for the tours and
 * exhibitions encapsulated here.
 *
 * For that reason, take care to set appropriate defaults for those properties
 * which are not provided directly.
 *
 * @author Philip Cohn-Cort (Fuzz)
 * @see SearchResultAdapter
 */
@JsonClass(generateAdapter = false)
data class ApiSearchResult(
        @Json(name = "artworks") val artworks: List<ApiSearchContent.SearchedArtwork>?,
        @Json(name = "tours") val tours: List<SearchedTour>?,
        @Json(name = "exhibitions") val exhibitions: List<ArticExhibition>?
)

// TODO: Rename below classes? Create JsonAdapter.Factory or something polymorphic?

@JsonClass(generateAdapter = true)
data class ApiSearchResultRawA(
        @Json(name = "data") val internalData: List<ApiSearchContent.SearchedArtwork>
)

@JsonClass(generateAdapter = true)
data class ApiSearchResultRawT(
        @Json(name = "data") val internalData: List<SearchedTour>
)

@JsonClass(generateAdapter = true)
data class ApiSearchResultRawE(
        @Json(name = "data") val internalData: List<ArticExhibition>
)


sealed class ApiSearchContent {
    /**
     * [artworkId] maps directly to [edu.artic.db.models.ArticSearchObject.searchObjects]
     */
    @JsonClass(generateAdapter = true)
    data class SearchedArtwork(
            @Json(name = "id") val artworkId: Int,
            @Json(name = "is_on_view") val isOnView: Boolean,
            @Json(name = "title") val title: String,
            @Json(name = "artist_display") val artist_display: String,
            @Json(name = "image_id") val image_id: String?,
            @Json(name = "gallery_id") val gallery_id: String,
            @Json(name = "latlon") val latlon: String?
    ) : ApiSearchContent()

    @JsonClass(generateAdapter = true)
    data class SearchedTour(
            @Json(name = "id") val tourId: Int
    ) : ApiSearchContent()
}
