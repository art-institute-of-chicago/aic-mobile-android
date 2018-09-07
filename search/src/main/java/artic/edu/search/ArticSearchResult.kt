package artic.edu.search

import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour

data class ArticSearchResult(
        var suggestions: List<String>,
        val artworks: List<ApiSearchResult.ArticSearchedArtwork>,
        val tours: List<ArticTour>,
        val exhibitions: List<ArticExhibition>
)