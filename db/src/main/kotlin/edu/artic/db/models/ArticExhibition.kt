package edu.artic.db.models

data class ArticExhibition(
        val title: String?,
        val image_filename: String?,
        val image_url: String?,
        val image_filemime: String?,
        val image_filesize: String?,
        val image_width: String?,
        val image_height: String?,
        val exhibition_id: String,
        val sort: Int
)