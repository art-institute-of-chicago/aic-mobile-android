package edu.artic.db

import edu.artic.db.models.ArticBlobData

sealed class AppDataState {
    class Downloading(val progress: Float) : AppDataState()
    class Done(val result: ArticBlobData) : AppDataState()
    object Empty : AppDataState()
}