package artic.edu.localization.ui

import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.localization.LanguageSelector
import edu.artic.localization.SPANISH
import edu.artic.localization.nameOfLanguageForAnalytics
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class LanguageSettingsViewModel @Inject constructor(
        val languageSelector: LanguageSelector,
        private val analyticsTracker: AnalyticsTracker,
        private val languageSettingsPrefManager: LanguageSettingsPrefManager
) : BaseViewModel() {


    val appLocale: Subject<Locale> = BehaviorSubject.create()
    val selectedLocale: Subject<Locale> = PublishSubject.create()

    init {
        appLocale.onNext(languageSelector.getAppLocale())

        /**
         * Fetch the app locale.
         */
        appLocale
                .distinctUntilChanged()
                .subscribe {
                    languageSelector.setDefaultLanguageForApplication(it)
                }.disposedBy(disposeBag)

        /**
         * Log analytics language changed event when user switches the language.
         */
        selectedLocale.subscribeBy {locale->
            analyticsTracker.reportEvent(
                    EventCategoryName.Language,
                    AnalyticsAction.languageChanged,
                    locale.nameOfLanguageForAnalytics()
            )
        }.disposedBy(disposeBag)

    }

    private fun changeLocale(locale: Locale) {
        /**
         * Log language selected event if user has not selected any language yet.
         */
        val userSelectedLanguage = languageSettingsPrefManager.userSelectedLanguage
        appLocale.onNext(locale)

        if (!userSelectedLanguage) {
            analyticsTracker.reportEvent(
                    EventCategoryName.Language,
                    AnalyticsAction.languageSelected,
                    locale.nameOfLanguageForAnalytics()
            )
            languageSettingsPrefManager.userSelectedLanguage = true
        } else {
            selectedLocale.onNext(locale)
        }

    }

    fun onEnglishLanguageSelected() {
        changeLocale(Locale.ENGLISH)
    }

    fun onSpanishLanguageSelected() {
        changeLocale(SPANISH)
    }

    fun onChineseLanguageSelected() {
        changeLocale(Locale.CHINESE)
    }
}