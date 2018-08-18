package edu.artic.localization;

import android.content.Context
import android.content.res.Configuration
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

    fun underlyingLanguage() : String?

    fun underlyingLocale(): Locale {
        return Locale.forLanguageTag(underlyingLanguage())
    }

    fun userFriendlyLanguage(forThisView: Context): CharSequence {
        val current = forThisView.resources.configuration

        return forThisView.createConfigurationContext(
                Configuration(current).apply {
                    setLocale(underlyingLocale())
                }
        ).getText(R.string.name_of_this_language)
    }
}
