package artic.edu.search

import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.ArticTour

data class ArticSearchResult(
        var searchTerm : String,
        var suggestions: List<String>,
        val artworks: List<ArticSearchArtworkObject>,
        val tours: List<ArticTour>,
        val exhibitions: List<ArticExhibition>
)