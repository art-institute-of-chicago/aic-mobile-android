package edu.artic.localization;

import java.util.*

/**
 * Common interface for DAO models which can be treated like translations of some sort of media.
 *
 * This must be an `interface` and not a proper superclass due
 * to [limitations in Moshi][com.squareup.moshi.JsonClass].
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
interface BaseTranslation {

    open fun underlyingLanguage() : String?

    fun underlyingLocale(): Locale {
        return Locale.forLanguageTag(underlyingLanguage())
    }
}
