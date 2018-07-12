package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.models.ArticBlobData

sealed class AppDataState {
    class Downloading(val progress: Float) : AppDataState()
    class Done(val result: Result<ArticBlobData>) : AppDataState()
    object Empty : AppDataState()
}