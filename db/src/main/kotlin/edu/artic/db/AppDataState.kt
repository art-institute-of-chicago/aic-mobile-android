package edu.artic.db

import edu.artic.db.models.ArticAppData

sealed class AppDataState {
    class Downloading(val progress: Float) : AppDataState()
    class Done(val result: ArticAppData, val headers: Map<String, List<String>>) : AppDataState()
    object Empty : AppDataState()
}