package edu.artic.localization

import android.content.Context
import edu.artic.base.BasePreferencesManager
import java.util.*

/**
 * Preferences re: what language to use.
 *
 * This would the place to store preferred measurement units and timezone.
 *
 * @author Piotr Leja (Fuzz)
 */
class LocalizationPreferences(context: Context)
    : BasePreferencesManager(context, "localization") {

    var preferredAppLocale: Locale
        set(given) = putString(
                "application_locale",
                given.toLanguageTag()
        )
        get() = Locale.forLanguageTag(getString(
                "application_locale",
                Locale.getDefault().toLanguageTag()
        )).orFallback()

}

/**
 * Switch to a given `fallback` if this object doesn't have a language.
 *
 * Call this only if we're doing some sort of locale-aware work; for
 * API calls, you should always be hardcoding the Locale you agreed
 * upon with the other party.
 *
 * @param fallback (optional) something with the language to use if
 * we don't have one
 * @return this object or `fallback` as described above
 */
fun Locale?.orFallback(fallback: Locale = Locale.ENGLISH) : Locale {
    return if (this == null || this.hasNoLanguage()) {
        fallback
    } else {
        this
    }
}

/**
 * Returns the name of this language, as used by our analytics engine.
 *
 * We use short-cut logic for known values, and fallback to [Locale.getDisplayLanguage]
 * otherwise.
 */
fun Locale.nameOfLanguageForAnalytics(): String {
    return when (language) {
        Locale.ENGLISH.language -> "English"
        SPANISH.language -> "Spanish"
        Locale.CHINESE.language -> "Chinese"
        else -> getDisplayLanguage(Locale.ENGLISH)
    }
}

val SPANISH: Locale
    get() = Locale.forLanguageTag("es")

/**
 * Check whether our [language][Locale.toLanguageTag] is the 'undefined' constant.
 */
fun Locale.hasNoLanguage() =
        this.language.isEmpty() || this.toLanguageTag() == "und"
