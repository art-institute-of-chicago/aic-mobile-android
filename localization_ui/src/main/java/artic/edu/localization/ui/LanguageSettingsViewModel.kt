package artic.edu.localization.ui

import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.localization.LanguageSelector
import edu.artic.localization.SPANISH
import edu.artic.localization.nameOfLanguageForAnalytics
import edu.artic.viewmodel.BaseViewModel
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
        private val analyticsTracker: AnalyticsTracker
) : BaseViewModel() {


    val appLocale: Subject<Locale> = BehaviorSubject.create()
    val selectedLocale: Subject<Locale> = PublishSubject.create()

    init {

        appLocale.onNext(languageSelector.getAppLocale())

        appLocale
                .distinctUntilChanged()
                .subscribe {
                    analyticsTracker.reportEvent(
                            EventCategoryName.Language,
                            AnalyticsAction.languageSelected,
                            it.nameOfLanguageForAnalytics()
                    )
                    languageSelector.setDefaultLanguageForApplication(it)

                }.disposedBy(disposeBag)

    }

    private fun changeLocale(locale: Locale) {
        appLocale.onNext(locale)
        selectedLocale.onNext(locale)
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