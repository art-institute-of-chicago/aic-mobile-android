package edu.artic.localization

import io.reactivex.subjects.Subject
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * This class hosts zero or more [RX subjects][Subject] related to language.
 *
 * Use this instead of going directly to [the default Locale][java.util.Locale.getDefault].
 *
 * @see SpecifiesLanguage
 */
class LanguageSelector(private val prefs: LocalizationPreferences) {

    private val appLocaleRef: AtomicReference<Locale> = AtomicReference(prefs.preferredAppLocale)

    fun setDefaultLanguageForApplication(lang: Locale) {
        if (lang.hasNoLanguage()) {
            if (BuildConfig.DEBUG) {
                throw IllegalArgumentException("Please ensure your chosen locale (\"${lang.language}\") actually includes a language.")
            }
        } else {
            prefs.preferredAppLocale = lang
            appLocaleRef.set(lang)
        }
    }

    /**
     * Check whether we have defined a value for [LocalizationPreferences.preferredAppLocale] -
     * when this returns false at app startup, consider asking the user to choose one.
     */
    fun isAppLocaleSet(): Boolean {
        return prefs.getString("application_locale", null) != null
    }

    /**
     * Returns the locally cached 'app-wide' [Locale].
     */
    fun getAppLocale(): Locale {
        return appLocaleRef.get()
    }

    fun setTourLanguage(proposedTourLocale: Locale) {
        if (proposedTourLocale.hasNoLanguage()) {
            prefs.remove(LocalizationPreferences.PREF_TOUR_LOCALE)
        } else {
            prefs.tourLocale = proposedTourLocale
        }
    }

    /**
     * Returns the first translation we can find in `languages` that belongs to the
     * same locale as [appLocaleRef]. If no such exists, we return the first language
     * in the list.
     *
     * As per the requirements laid out in [Locale.getLanguage], we only convert
     * languages that have been normalized through [Locale.forLanguageTag]. We
     * cannot necessarily trust the original Strings returned by the API.
     */
    fun <T : SpecifiesLanguage> selectFrom(languages: List<T>, prioritizeTour: Boolean = false): T {
        val appLocale: Locale = appLocaleRef.get()
        // TODO: Investigate possibility of replacing the list with a `LocaleList`

        val priority: T? = if (prioritizeTour) {
            findIn(languages, prefs.tourLocale)
        } else {
            null
        }

        return priority ?: findIn(languages, appLocale) ?: languages.first()

    }

    private fun <T : SpecifiesLanguage> findIn(languages: List<T>, wanted: Locale): T? {
        return languages.firstOrNull {
            // This is very much intentional. Read the method docs fully before changing.
            it.underlyingLocale().language == wanted.language
        }
    }

}