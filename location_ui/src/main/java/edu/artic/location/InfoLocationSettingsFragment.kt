package edu.artic.location

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.location_ui.R
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_location_settings.*
import kotlin.reflect.KClass


class InfoLocationSettingsFragment : BaseViewModelFragment<InfoLocationSettingsViewModel>() {
    override val viewModelClass: KClass<InfoLocationSettingsViewModel> = InfoLocationSettingsViewModel::class
    override val title = R.string.location_title
    override val layoutResId: Int = R.layout.fragment_location_settings
    override val screenName: ScreenName? = ScreenName.LocationSettings


    override fun setupBindings(viewModel: InfoLocationSettingsViewModel) {
        super.setupBindings(viewModel)
        // TODO: Bind resource ids directly to TextView (don't call `::getString` explicitly here)
        viewModel.buttonType
                .map {
                    val stringId = when (it) {
                        InfoLocationSettingsViewModel.ButtonType.LocationNeverRequested -> {
                            R.string.locationSettingsLocationNeverAsked
                        }
                        InfoLocationSettingsViewModel.ButtonType.LocationEnabled -> {
                            R.string.locationSettingsLocationAllowed
                        }
                        InfoLocationSettingsViewModel.ButtonType.LocationDisabled -> {
                            R.string.locationSettingsLocationDenied
                        }
                        InfoLocationSettingsViewModel.ButtonType.LocationServiceOff -> {
                            R.string.locationSettingsLocationServiceOff
                        }
                    }
                    return@map getString(stringId)
                }
                .bindToMain(locationSettingsButton.text())
                .disposedBy(disposeBag)

        // TODO: Throttle click events
        locationSettingsButton
                .clicks()
                .subscribe {
                    viewModel.onClickButton()
                }
                .disposedBy(disposeBag)

        searchIcon.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onClickSearch()
                }.disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: InfoLocationSettingsViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filterFlatMap({ it is Navigate.Forward }, { (it as Navigate.Forward).endpoint })
                .subscribe {
                    // TODO: Investigate whether perhaps we want to use `::startActivityForResult` in these branches instead
                    when (it) {
                        InfoLocationSettingsViewModel.NavigationEndpoint.Settings -> {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", requireContext().packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        InfoLocationSettingsViewModel.NavigationEndpoint.LocationServiceSettings -> {
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        InfoLocationSettingsViewModel.NavigationEndpoint.Search -> {
                            val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                            startActivity(intent)
                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }
}