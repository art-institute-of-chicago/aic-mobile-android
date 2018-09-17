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

    companion object {
        const val PREF_TOUR_LOCALE = "tour_locale"
    }

    var preferredAppLocale: Locale
        set(given) = putString(
                "application_locale",
                given.toLanguageTag()
        )
        get() = Locale.forLanguageTag(getString(
                "application_locale",
                Locale.getDefault().toLanguageTag()
        )).orFallback()

    private var innerTourLocale: String? = null

    /**
     * Public access to the current tour language.
     *
     * Internally managed via [innerTourLocale].
     */
    var tourLocale: Locale
        set(given) {
            innerTourLocale = given.toLanguageTag()
        }
        get() = Locale.forLanguageTag(
                innerTourLocale ?:
                preferredAppLocale.toLanguageTag()
        ).orFallback(preferredAppLocale)
}

