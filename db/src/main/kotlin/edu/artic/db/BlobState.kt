package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.db.models.ArticBlobData

sealed class BlobState {
    class Downloading(val progress: Float) : BlobState()
    class Done(val result : Result<ArticBlobData>) : BlobState()
    class Empty() : BlobState()
}