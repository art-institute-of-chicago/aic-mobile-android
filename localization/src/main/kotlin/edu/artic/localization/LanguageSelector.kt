package edu.artic.localization

import io.reactivex.subjects.BehaviorSubject
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

    /**
     * Broadcast point for changes in the current language.
     *
     * See [getAppLocale] for the language _right now_ and
     * [appLanguageWithUpdates] if you need both that and
     * the live updates.
     */
    private val currentLanguage: Subject<Locale> = BehaviorSubject.createDefault(getAppLocale())


    fun setDefaultLanguageForApplication(lang: Locale) {
        if (lang.hasNoLanguage()) {
            if (BuildConfig.DEBUG) {
                throw IllegalArgumentException("Please ensure your chosen locale (\"${lang.language}\") actually includes a language.")
            }
        } else {
            prefs.preferredAppLocale = lang
            appLocaleRef.set(lang)
            currentLanguage.onNext(lang)
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
        return Locale.forLanguageTag(appLocaleRef.get().language)
    }

    fun setTourLanguage(proposedTourLocale: Locale) {
        if (proposedTourLocale.hasNoLanguage()) {
            prefs.tourLocale = Locale.ROOT
        } else {
            prefs.tourLocale = proposedTourLocale
        }
    }

    /**
     * The returned object is given an initial value of [getAppLocale], and it will
     * reflect the value of the latest event published via [currentLanguage] after that point.
     *
     * TODO: This and some of the ::onViewRecycled code used with the :adapter module really belong in a dedicated file. For now, though, this can stay here.
     *
     * @param disposeBag this should be the same composite that you'll use to dispose of
     * subscriptions on the returned object. We use it here to hook up [currentLanguage]
     * with your existing lifecycle callbacks
     */
    fun appLanguageWithUpdates(): BehaviorSubject<Locale> {
        return currentLanguage as BehaviorSubject<Locale>
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