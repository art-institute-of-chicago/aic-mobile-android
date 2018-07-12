package edu.artic.db.models

data class ArticMapAnnotation(
        val title: String,
        val status: String,
        val nid: String,
        val type: String,
        val translations: List<Any>,
        val location: String,
        val latitude: String,
        val longitude: String,
        val floor: String,
        val description: String?,
        val label: String?,
        val annotation_type: String?,
        val text_type: String?,
        val amenity_type: String?,
        val image_filename: String?,
        val image_url: String?,
        val image_filemime: String?,
        val image_filesize: String?,
        val image_width: String?,
        val image_height: String?
)