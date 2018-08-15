package edu.artic.localization

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.*

/**
 * This class hosts one or more [RX subjects][Subject] related to language.
 *
 * Use this instead of going directly to [the default Locale][java.util.Locale.getDefault].
 */
class LanguageSelector(private val prefs: LocalizationPreferences) {

    val appLocaleObservable: Subject<Locale> = BehaviorSubject.createDefault(prefs.preferredAppLocale)

    fun setDefaultLanguageForApplication(lang: Locale) {
        if (lang.hasNoLanguage()) {
            if (BuildConfig.DEBUG) {
                throw IllegalArgumentException("Please ensure your chosen locale (\"${lang.language}\") actually includes a language.")
            }
        } else {
            prefs.preferredAppLocale = lang
            appLocaleObservable.onNext(lang)
        }
    }

}