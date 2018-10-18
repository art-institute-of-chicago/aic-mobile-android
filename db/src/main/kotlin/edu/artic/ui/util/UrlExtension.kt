package edu.artic.ui.util

import android.net.Uri
import edu.artic.db.BuildConfig
import edu.artic.db.models.ArticDataObject

/**
 * Use this to ensure that your image urls are always hitting the CDN.
 *
 * Especially handy for IIIF resources.
 *
 * FIXME: Use [edu.artic.db.models.ArticDataObject.imageServerUrl] instead of [BuildConfig.CDN_HOST]
 */
fun String.asCDNUri() : String {
    val parsed = Uri.parse(this)
    return when {
        parsed.authority == ArticDataObject.IMAGE_SERVER_URL -> parsed.buildUpon()
                .authority(ArticDataObject.IMAGE_SERVER_URL)
                .build()
        else -> parsed
    }.toString()
}

/**
 * Returns this if it is not null, or else [Uri.EMPTY] if it _is_ `null`.
 *
 * This method is inspired by [String.orEmpty].
 */
inline fun Uri?.orEmpty(): Uri {
    return this ?: Uri.EMPTY
}

/**
 * Checks whether this is `==` to [Uri.EMPTY].
 *
 * This method is inspired by [String.isEmpty].
 */
inline fun Uri.isEmpty(): Boolean {
    return this == Uri.EMPTY
}

/**
 * Checks whether this is `!=` to [Uri.EMPTY].
 *
 * This method is inspired by [String.isNotEmpty].
 */
inline fun Uri.isNotEmpty(): Boolean {
    return !isEmpty()
}