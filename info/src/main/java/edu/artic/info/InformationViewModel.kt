package edu.artic.info

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class InformationViewModel @Inject constructor(val analyticsTracker: AnalyticsTracker, val dataObjectDao: ArticDataObjectDao) : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object AccessMemberCard : NavigationEndpoint()
        object Search : NavigationEndpoint()
        class JoinNow(val url: String) : NavigationEndpoint()
    }

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
                .bindTo(navigateTo)
                .disposedBy(disposeBag)
    }
}
