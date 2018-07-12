package edu.artic.db.models

data class ArticSearchObject(
        val search_strings: Map<String, String>,
        val search_objects: List<Int>
)