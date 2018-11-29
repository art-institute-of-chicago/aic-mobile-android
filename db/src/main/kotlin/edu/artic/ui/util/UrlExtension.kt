package edu.artic.ui.util

/**
 * Use this to ensure that your image urls are always hitting the CDN.
 *
 * Especially handy for IIIF resources.
 */
@Deprecated(
        message = "This method does nothing, as CDN Urls are now provided directly by the API.",
        replaceWith = ReplaceWith("toString()")
)
fun String.asCDNUri() : String {
    return this
}