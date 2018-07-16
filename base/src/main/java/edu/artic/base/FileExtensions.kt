package edu.artic.base

import android.content.res.AssetManager
import java.nio.charset.Charset

/**
 * @author Sameer Dhakal (Fuzz)
 */
fun AssetManager.fileAsString(subdirectory: String, filename: String): String {
    return open("$subdirectory/$filename").use {
        it.readBytes().toString(Charset.defaultCharset())
    }
}