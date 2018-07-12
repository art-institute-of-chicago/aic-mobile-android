package edu.artic.db.models

data class ArticGallery(
        val title: String,
        val status: String,
        val nid: String,
        val type: String,
        val location: String,
        val latitude: Double,
        val longitude: Double,
        val floor: String,
        val title_t: String,
        val gallery_id: String,
        val is_boosted: Boolean,
        val thumbnail: String,
        val closed: Boolean,
        val number: String,
        val category_titles: List<String>
)