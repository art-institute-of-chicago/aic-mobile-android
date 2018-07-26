package edu.artic.base.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html

/**
 *@author Sameer Dhakal (Fuzz)
 */

/**
 * Use this to create intents for deep-linking.
 *
 * E.g.
 * `"edu.artic.home".asDeepLinkIntent()`
 *
 */
fun String.asDeepLinkIntent(action: String = Intent.ACTION_VIEW, schema: String = "artic"): Intent {
    return Intent(action, Uri.parse("$schema://${this}"))
}

fun String.fromHtml() : CharSequence {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(this)
    }
}