package edu.artic.location

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_location_settings.*
import kotlin.reflect.KClass


class InfoLocationSettingsFragment : BaseViewModelFragment<InfoLocationSettingsViewModel>() {
    override val viewModelClass: KClass<InfoLocationSettingsViewModel> = InfoLocationSettingsViewModel::class
    override val title = R.string.location_title
    override val layoutResId: Int = R.layout.fragment_location_settings
    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.LocationSettings


    override fun setupBindings(viewModel: InfoLocationSettingsViewModel) {
        super.setupBindings(viewModel)
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

        locationSettingsButton
                .clicks()
                .subscribe {
                    viewModel.onClickButton()
                }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: InfoLocationSettingsViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filterFlatMap({ it is Navigate.Forward }, { (it as Navigate.Forward).endpoint })
                .subscribe {
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
                    }
                }.disposedBy(navigationDisposeBag)
    }
}