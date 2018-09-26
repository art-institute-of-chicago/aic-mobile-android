package edu.artic.location

import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import javax.inject.Inject

class LocationPromptViewModel @Inject constructor(
        private val locationService: LocationService,
        locationPreferenceManager: LocationPreferenceManager
) : NavViewViewModel<LocationPromptViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    init {
        locationPreferenceManager.hasSeenLocationPrompt = true
    }

    fun onClickNotNowButton() {
        navigateTo.onNext(Navigate.Back())
    }

    fun onClickOk() {
        locationService.requestLocationPermissions()
        navigateTo.onNext(Navigate.Back())
    }
}