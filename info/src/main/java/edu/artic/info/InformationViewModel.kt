package edu.artic.info

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.ArticGeneralInfo
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
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
        private val generalInfoDao: GeneralInfoDao,
        @Named("VERSION") buildVersion: String
)
    : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object AccessMemberCard : NavigationEndpoint()
        object LanguageSettings : NavigationEndpoint()
        object Search : NavigationEndpoint()
        class JoinNow(val url: String) : NavigationEndpoint()
        object MuseumInformation : NavigationEndpoint()
        object LocationSettings : NavigationEndpoint()
    }

    val buildVersion: Subject<String> = BehaviorSubject.createDefault(buildVersion)
    val generalInfo: Subject<ArticGeneralInfo.Translation> = BehaviorSubject.create()

    init {

        Observables.combineLatest(
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

    fun onClickJoinNow() {
        analyticsTracker.reportEvent(ScreenCategoryName.Information, AnalyticsAction.memberJoinPressed)
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

    fun onClickLanguageSettings() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.LanguageSettings))
    }
}
