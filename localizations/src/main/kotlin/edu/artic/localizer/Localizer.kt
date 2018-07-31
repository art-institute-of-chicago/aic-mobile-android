package edu.artic.localizer

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class Localizer(private val localizerPreferences: LocalizerPreferences) {

    val appLanguage: Subject<String> = BehaviorSubject.createDefault(localizerPreferences.applicationLanguage)

    fun setDefaultLanguageForApplication(languageCode: String) {
        localizerPreferences.applicationLanguage = languageCode
        appLanguage.onNext(languageCode)
    }

}