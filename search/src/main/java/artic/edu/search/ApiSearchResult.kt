package artic.edu.search

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour

@JsonClass(generateAdapter = true)
data class ApiSearchResult(
        @Json(name = "artworks") val artworks: List<ArticSearchedArtwork>?,
        @Json(name = "tours") val tours: List<ArticTour>?,
        @Json(name = "exhibitions") val exhibitions: List<ArticExhibition>?
) {

    /**
     * [artworkId] maps directly to [edu.artic.db.models.ArticSearchObject.searchObjects]
     */
    data class ArticSearchedArtwork(
            @Json(name = "id") val artworkId: Int,
            @Json(name = "is_on_view") val isOnView: Boolean
    )
}