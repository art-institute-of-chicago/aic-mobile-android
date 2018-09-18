package edu.artic.location

import edu.artic.viewmodel.NavViewViewModel
import javax.inject.Inject

class InfoLocationSettingsViewModel @Inject constructor() : NavViewViewModel<InfoLocationSettingsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint
}