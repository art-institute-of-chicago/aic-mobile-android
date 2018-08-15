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

    val preferredAppLocale: Subject<Locale> = BehaviorSubject.createDefault(prefs.preferredAppLocale)

    fun setDefaultLanguageForApplication(lang: Locale) {
        prefs.preferredAppLocale = lang
        preferredAppLocale.onNext(lang)
    }

}