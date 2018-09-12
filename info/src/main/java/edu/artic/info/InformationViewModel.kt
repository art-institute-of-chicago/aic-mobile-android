package edu.artic.info

import android.support.annotation.StringRes
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class InformationViewModel @Inject constructor() : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object AccessMemberCard : NavigationEndpoint()
        object Search : NavigationEndpoint()
        class JoinNow(@StringRes val url: Int) : NavigationEndpoint()
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    fun joinNow() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.JoinNow(R.string.joinUrl)))
    }
}
