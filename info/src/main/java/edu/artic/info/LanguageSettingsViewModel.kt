package edu.artic.info

import com.fuzz.rx.disposedBy
import edu.artic.localization.LanguageSelector
import edu.artic.localization.LocalizationPreferences
import edu.artic.localization.SPANISH
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.*
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class LanguageSettingsViewModel @Inject constructor(
        val languageSelector: LanguageSelector
) : BaseViewModel() {


    val selectedLanguage: Subject<Locale> = BehaviorSubject.create()

    init {

        selectedLanguage.onNext(languageSelector.getAppLocale())

        selectedLanguage
                .distinctUntilChanged()
                .subscribe {
                    languageSelector.setDefaultLanguageForApplication(it)

                }.disposedBy(disposeBag)

    }


    fun onEnglishLanguageSelected() {
        selectedLanguage.onNext(Locale.ENGLISH)
    }

    fun onSpanishLanguageSelected() {
        selectedLanguage.onNext(SPANISH)
    }

    fun onChineseLanguageSelected() {
        selectedLanguage.onNext(Locale.CHINESE)
    }

}