package edu.artic.info


import android.content.Intent
import android.net.Uri
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textRes
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.info.databinding.FragmentMuseumInformationBinding
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
//import kotlinx.android.synthetic.main.fragment_museum_information.*
import javax.inject.Inject
import kotlin.reflect.KClass


class MuseumInformationFragment :
    BaseViewModelFragment<FragmentMuseumInformationBinding, MuseumInformationViewModel>() {

    @Inject
    lateinit var customTabManager: CustomTabManager

    override val viewModelClass: KClass<MuseumInformationViewModel> =
        MuseumInformationViewModel::class

    override val title = R.string.info_museum_info_action


    override val screenName: ScreenName = ScreenName.MuseumInformation

    override fun setupBindings(viewModel: MuseumInformationViewModel) {
        super.setupBindings(viewModel)
        viewModel.museumHours
            .bindToMain(binding.museumHours.text())
            .disposedBy(disposeBag)

        viewModel.museumPhone
            .bindToMain(binding.museumPhone.textRes())
            .disposedBy(disposeBag)

        viewModel.museumAddress
            .bindToMain(binding.museumAddress.textRes())
            .disposedBy(disposeBag)

        binding.museumAddress.clicks()
            .subscribe {
                viewModel.onMuseumAddressClicked()
            }.disposedBy(disposeBag)

        binding.museumPhone.clicks()
            .subscribe {
                viewModel.onPhoneNumberClicked()
            }.disposedBy(disposeBag)

        binding.searchIcon.clicks()
            .defaultThrottle()
            .subscribe {
                viewModel.onClickSearch()
            }.disposedBy(disposeBag)

        requireActivity().title = resources.getString(R.string.info_museum_info_action)

    }

    override fun setupNavigationBindings(viewModel: MuseumInformationViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
            .subscribe {
                if (it is Navigate.Forward) {
                    when (it.endpoint) {
                        is MuseumInformationViewModel.NavigationEndpoint.CallMuseum -> {
                            val phoneId =
                                (it.endpoint as MuseumInformationViewModel.NavigationEndpoint.CallMuseum).phone
                            val phone = getString(phoneId)
                            val intent = Intent(Intent.ACTION_DIAL)
                            intent.data = Uri.parse("tel:$phone")
                            val chooser = Intent.createChooser(
                                intent,
                                resources.getString(R.string.info_dial_prompt)
                            )
                            startActivity(chooser)
                        }
                        is MuseumInformationViewModel.NavigationEndpoint.ShowMuseumInMap -> {
                            val url = getString(R.string.info_museum_google_map_query)
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            val chooser = Intent.createChooser(
                                intent,
                                resources.getString(R.string.info_map_prompt)
                            )
                            startActivity(chooser)
                        }
                        is MuseumInformationViewModel.NavigationEndpoint.Search -> {
                            val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                            startActivity(intent)
                        }
                    }
                }
            }.disposedBy(navigationDisposeBag)
    }
}
