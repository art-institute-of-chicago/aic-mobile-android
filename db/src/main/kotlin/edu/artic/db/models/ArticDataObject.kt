package edu.artic.db.models

data class ArticDataObject(
        val image_server_url: String,
        val data_api_url: String,
        val exhibitions_endpoint: String,
        val artworks_endpoint: String,
        val galleries_endpoint: String,
        val images_endpoint: String,
        val events_endpoint: String,
        val autocomplete_endpoint: String,
        val tours_endpoint: String,
        val multisearch_endpoint: String,
        val membership_url: String,
        val website_url: String,
        val tickets_url: String
)