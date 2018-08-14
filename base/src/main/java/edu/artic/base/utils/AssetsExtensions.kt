package edu.artic.base.utils

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.AnyThread
import java.io.InputStream
import java.nio.charset.Charset


/*
 * This file defines extension functions for working with [AssetManager].
 *
 * Loading files, defining resources, IO stuff like that.
 */

/**
 * This particular function reads the entirety of a file into memory as a [String]
 *
 * @author Sameer Dhakal (Fuzz)
 */
fun AssetManager.fileAsString(subdirectory: String, filename: String): String {
    return open("$subdirectory/$filename").use {
        it.readBytes().toString(Charset.defaultCharset())
    }
}

/**
 * Load a [Bitmap] from an asset file with the given name.
 *
 * We use the same file resolution strategy as [AssetManager.open]. This
 * method will throw an exception if there is no asset of the given name
 * or if that asset isn't Bitmap-compatible (`JPEG` and `PNG` formats are
 * all right, `SVG` isn't).
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
@AnyThread
fun AssetManager.loadBitmap(assetFilename: String, sampleSize: Int): Bitmap {
    val stream: InputStream = this.open(assetFilename)
    return stream.use { imageStream ->
        BitmapFactory.decodeStream(
                imageStream,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
        )
    }
}