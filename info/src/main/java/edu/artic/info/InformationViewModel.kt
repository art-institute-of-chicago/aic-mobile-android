package edu.artic.info

import edu.artic.viewmodel.NavViewViewModel
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class InformationViewModel @Inject constructor() : NavViewViewModel<InformationViewModel.NavigationEndpoint>() {
    sealed class NavigationEndpoint {
        class AccessMemberCard : NavigationEndpoint()
    }
}
