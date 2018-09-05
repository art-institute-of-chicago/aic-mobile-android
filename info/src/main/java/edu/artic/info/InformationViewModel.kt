package edu.artic.info

import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class InformationViewModel @Inject constructor() : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object AccessMemberCard : NavigationEndpoint()
        object Search: NavigationEndpoint()
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }
}
