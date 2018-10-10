package edu.artic.base.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Spanned

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

/**
 * Convert this (probably HTML-style) text into the [Spanned] equivalent.
 *
 * On Android N and higher you can override the parse mode with the [flags]
 * parameter. See [Html.fromHtml] and overloads thereof for more details.
 */
@SuppressLint("InlinedApi")
fun String.fromHtml(flags: Int = Html.FROM_HTML_MODE_LEGACY): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, flags)
    } else {
        Html.fromHtml(this)
    }
}

fun String.asUrlViewIntent(action: String = Intent.ACTION_VIEW): Intent {
    val fullUrl: String = if (!this.startsWith("http://") && !this.startsWith("https://")) {
        "https://$this"
    } else {
        this
    }
    return Intent(action, Uri.parse(fullUrl))
}

/**
 * If this
 * * is not null and
 * * is not the empty string `""` and
 * * doesn't just consist of spaces
 *
 * then this function just returns the String it was called on.
 *
 * Otherwise, we will return [alternate].
 */
fun String?.orIfNullOrBlank(alternate: String?): String? {
    return if (this.isNullOrBlank()) {
        alternate
    } else {
        this
    }
}

/**
 * Decodes html encoded strings preserving new line chars.
 */
fun String.filterHtmlEncodedText(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(replace("\n", "<br>"), Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        Html.fromHtml(replace("\n", "<br>")).toString()
    }.apply {
        replace("\\</br.*?>", "\\\n")
    }
}