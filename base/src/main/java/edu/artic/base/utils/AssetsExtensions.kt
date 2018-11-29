package edu.artic.base.utils

import android.content.res.AssetManager
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
fun AssetManager.fileAsString(subdirectory: String? = null, filename: String): String {
    val filePath = subdirectory?.let { "$it/$filename" } ?: filename
    return open(filePath).use {
        it.readBytes().toString(Charset.defaultCharset())
    }
}