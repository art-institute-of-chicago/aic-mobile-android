package edu.artic.location

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class InfoLocationSettingsViewModel @Inject constructor(private val locationService: LocationService) : NavViewViewModel<InfoLocationSettingsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object Settings : NavigationEndpoint()
        object LocationServiceSettings : NavigationEndpoint()
        object Search : NavigationEndpoint()
    }


    sealed class ButtonType {
        object LocationServiceOff : ButtonType()
        object LocationNeverRequested : ButtonType()
        object LocationEnabled : ButtonType()
        object LocationDisabled : ButtonType()
    }

    val buttonType: Subject<ButtonType> = BehaviorSubject.create()

    init {

        Observables.combineLatest(
                locationService.hasRequestedPermissionAlready,
                locationService.authorizationStatusDistinct,
                locationService.deviceLocationEnabledDistinct
        ) { hasAlreadyRequested, currentAuthStatus, locationEnabled ->

            return@combineLatest if (!locationEnabled) {
                ButtonType.LocationServiceOff
            } else if (!hasAlreadyRequested) {
                ButtonType.LocationNeverRequested
            } else {
                when (currentAuthStatus) {
                    LocationService.AuthorizationStatus.LocationAllowed -> {
                        ButtonType.LocationEnabled
                    }
                    is LocationService.AuthorizationStatus.LocationDenied -> {
                        ButtonType.LocationDisabled
                    }
                }
            }
        }.bindTo(buttonType).disposedBy(disposeBag)

    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    fun onClickButton() {
        if ((buttonType as BehaviorSubject).hasValue()) {
            when (buttonType.value) {
                InfoLocationSettingsViewModel.ButtonType.LocationServiceOff -> {
                    navigateTo.onNext(Navigate.Forward(NavigationEndpoint.LocationServiceSettings))
                }
                InfoLocationSettingsViewModel.ButtonType.LocationNeverRequested -> {
                    locationService.requestLocationPermissions()
                }
                InfoLocationSettingsViewModel.ButtonType.LocationEnabled -> {
                    navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Settings))
                }
                InfoLocationSettingsViewModel.ButtonType.LocationDisabled -> {
                    navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Settings))
                }
                null -> {
                    //This should never be called
                }
            }
        }
    }
}