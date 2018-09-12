package edu.artic.info

import android.support.annotation.StringRes
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class InformationViewModel @Inject constructor(val analyticsTracker: AnalyticsTracker) : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object AccessMemberCard : NavigationEndpoint()
        object Search : NavigationEndpoint()
        class JoinNow(@StringRes val url: Int) : NavigationEndpoint()
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    fun joinNow() {
        analyticsTracker.reportEvent(ScreenCategoryName.Information, AnalyticsAction.memberJoinPressed)
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.JoinNow(R.string.joinUrl)))
    }
}
