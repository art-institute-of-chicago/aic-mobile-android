package edu.artic.info

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * @author Sameer Dhakal (Fuzz)
 */
class InformationViewModel @Inject constructor(val analyticsTracker: AnalyticsTracker,
                                               val dataObjectDao: ArticDataObjectDao,
                                               @Named("VERSION") buildVersion: String)
    : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object AccessMemberCard : NavigationEndpoint()
        object Search : NavigationEndpoint()
        class JoinNow(val url: String) : NavigationEndpoint()
        object MuseumInformation : NavigationEndpoint()
    }

    val buildVersion: Subject<String> = BehaviorSubject.createDefault(buildVersion)

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    fun onClickJoinNow() {
        analyticsTracker.reportEvent(ScreenCategoryName.Information, AnalyticsAction.memberJoinPressed)
        dataObjectDao.getDataObject()
                .toObservable()
                .take(1)
                .map { it.membershipUrl.orEmpty() }
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
}
