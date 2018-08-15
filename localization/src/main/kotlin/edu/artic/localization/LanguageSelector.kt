package edu.artic.localization

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * This class hosts one or more [RX subjects][Subject] related to language.
 *
 * Use this instead of going directly to [the default Locale][java.util.Locale.getDefault].
 */
class LanguageSelector(private val prefs: LocalizationPreferences) {

    val appLanguage: Subject<String> = BehaviorSubject.createDefault(prefs.applicationLanguage)

    fun setDefaultLanguageForApplication(languageCode: String) {
        prefs.applicationLanguage = languageCode
        appLanguage.onNext(languageCode)
    }

}