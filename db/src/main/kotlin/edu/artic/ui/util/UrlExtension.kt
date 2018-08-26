package edu.artic.ui.util

import android.net.Uri
import edu.artic.db.BuildConfig

/**
 * Use this to ensure that your image urls are always hitting the CDN.
 *
 * FIXME: Use [edu.artic.db.models.ArticDataObject.imageServerUrl] instead of [BuildConfig.CDN_HOST]
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