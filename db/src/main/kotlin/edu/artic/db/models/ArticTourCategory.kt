package edu.artic.db.models

data class ArticTourCategory(
        val category: String,
        val translations: List<Translation>) {

    data class Translation(
            val language: String,
            val category: String
    )
}