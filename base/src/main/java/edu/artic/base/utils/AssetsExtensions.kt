package edu.artic.base.utils

import android.content.res.AssetManager
import java.nio.charset.Charset

/**
 * Extension functions for working with [AssetManager].
 *
 * Loading files, defining resources, IO stuff like that.
 *
 * @author Sameer Dhakal (Fuzz)
 */
fun AssetManager.fileAsString(subdirectory: String, filename: String): String {
    return open("$subdirectory/$filename").use {
        it.readBytes().toString(Charset.defaultCharset())
    }
}