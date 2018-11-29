package edu.artic.base.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.text.HtmlCompat
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan

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
 * Sometimes HTML content comes in with 3 or more blank lines in a row.
 *
 * This detects and replaces such occurrences with a single html `br` tag.
 */
fun String.trimDownBlankLines(): String {
    return replace("\n\n", "\n")
            .replace("\n", "<br/>")
            .replace("<br/><strong><br/></strong>", "<br/>")
}

/**
 * Convert this (probably HTML-style) text into the [Spanned] equivalent.
 *
 * On Android N and higher you can override the parse mode with the [flags]
 * parameter. See [Html.fromHtml] and overloads thereof for more details.
 */
fun String.fromHtml(flags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY): CharSequence {
    val htmlText: Spanned = HtmlCompat.fromHtml(this, flags)

    return htmlText
            .asSpannable()
            .removeAnchors()
            .trim()
}

private fun Spanned.asSpannable(): Spannable {
    return this as? Spannable ?: SpannableString(this)
}

/**
 * As per ticket AIC-567, we must remove all visible `a` spans.
 */
private fun Spannable.removeAnchors(): Spannable {
    return this.apply {
        getSpans(
                0,
                length,
                ClickableSpan::class.java
        ).forEach(this::removeSpan)
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