package edu.artic.ui.util

import android.net.Uri
import edu.artic.ui.BuildConfig

/**
 * Use this to ensure that your image urls are always hitting the CDN.
 */
fun String.asCDNUri() : Uri {
    val parsed = Uri.parse(this)
    return when {
        parsed.authority == BuildConfig.OFFICIAL_IMAGE_HOST -> parsed.buildUpon()
                .authority(BuildConfig.CDN_HOST)
                .build()
        else -> parsed
    }
}