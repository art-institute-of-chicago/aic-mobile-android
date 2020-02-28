package edu.artic.info

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.ArticGeneralInfo
import edu.artic.localization.LanguageSelector
import edu.artic.localization.LocalizationPreferences
import edu.artic.localization.ui.LanguageSettingsPrefManager
import edu.artic.map.tutorial.TutorialPreferencesManager
import edu.artic.media.audio.AudioPrefManager
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import edu.artic.welcome.WelcomePreferencesManager
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Named

/**
 * @author Sameer Dhakal (Fuzz)
 */
class InformationViewModel @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val languageSelector: LanguageSelector,
        private val dataObjectDao: ArticDataObjectDao,
        generalInfoDao: GeneralInfoDao,
        private val languageSettingsPrefManager: LanguageSettingsPrefManager,
        private val localizationPreferences: LocalizationPreferences,
        private val tutorialPreferencesManager: TutorialPreferencesManager,
        private val audioPrefManager: AudioPrefManager,
        private val welcomePreferencesManager: WelcomePreferencesManager,
        @Named("VERSION") buildVersion: String
) : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class BuyTicket(val url: String) : NavigationEndpoint()
        object AccessMemberCard : NavigationEndpoint()
        object LanguageSettings : NavigationEndpoint()
        object Search : NavigationEndpoint()
        class JoinNow(val url: String) : NavigationEndpoint()
        object MuseumInformation : NavigationEndpoint()
        object LocationSettings : NavigationEndpoint()
        object ResetDevice : NavigationEndpoint()
    }

    val buildVersion: Subject<String> = BehaviorSubject.createDefault(buildVersion)
    val generalInfo: Subject<ArticGeneralInfo.Translation> = BehaviorSubject.create()

    init {

        Observables
                .combineLatest(
                        languageSelector.currentLanguage,
                        generalInfoDao.getGeneralInfo().toObservable()
                )
                .map { (_, generalInfo) ->
                    languageSelector.selectFrom(generalInfo.allTranslations())
                }
                .bindTo(generalInfo)
                .disposedBy(disposeBag)

    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    fun onBuyTicketClicked() {
        dataObjectDao.getDataObject()
                .toObservable()
                .take(1)
                .map { it.ticketsUrlAndroid }
                .filter { it.isNotEmpty() }
                .map { url -> Navigate.Forward(NavigationEndpoint.BuyTicket(url)) }
                .subscribe {
                    navigateTo.onNext(it)
                }
                .disposedBy(disposeBag)
    }

    fun onClickJoinNow() {
        analyticsTracker.reportEvent(EventCategoryName.Member, AnalyticsAction.memberJoinPressed)
        dataObjectDao.getDataObject()
                .toObservable()
                .take(1)
                .map { it.membershipUrlAndroid }
                .filter { it.isNotEmpty() }
                .map { Navigate.Forward(NavigationEndpoint.JoinNow(it)) }
                .subscribe {
                    navigateTo.onNext(it)
                }
                .disposedBy(disposeBag)
    }

    fun onMuseumInformationClicked() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.MuseumInformation))
    }

    fun onAccessMemberCardClicked() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.AccessMemberCard))
    }

    fun onClickLocationSettings() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.LocationSettings))
    }

    fun onClickResetDevice() {
        languageSettingsPrefManager.clear()
        localizationPreferences.clear()
        tutorialPreferencesManager.clear()
        audioPrefManager.clear()
        welcomePreferencesManager.clear()

        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ResetDevice))
    }

    fun onClickLanguageSettings() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.LanguageSettings))
    }
}
