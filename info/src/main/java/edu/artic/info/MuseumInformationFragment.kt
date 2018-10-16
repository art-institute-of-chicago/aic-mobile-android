package edu.artic.info


import android.content.Intent
import android.net.Uri
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textRes
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_museum_information.*
import javax.inject.Inject
import kotlin.reflect.KClass


class MuseumInformationFragment : BaseViewModelFragment<MuseumInformationViewModel>() {

    @Inject
    lateinit var customTabManager: CustomTabManager

    override val viewModelClass: KClass<MuseumInformationViewModel> = MuseumInformationViewModel::class

    override val title = R.string.museumInformation

    override val layoutResId: Int = R.layout.fragment_museum_information

    override val screenName: ScreenCategoryName = ScreenCategoryName.MuseumInformation

    override fun setupBindings(viewModel: MuseumInformationViewModel) {
        super.setupBindings(viewModel)
        viewModel.museumHours
                .bindToMain(museumHours.text())
                .disposedBy(disposeBag)

        viewModel.museumPhone
                .bindToMain(museumPhone.textRes())
                .disposedBy(disposeBag)

        viewModel.museumAddress
                .bindToMain(museumAddress.textRes())
                .disposedBy(disposeBag)

        museumAddress.clicks()
                .subscribe {
                    viewModel.onMuseumAddressClicked()
                }.disposedBy(disposeBag)

        museumPhone.clicks()
                .subscribe {
                    viewModel.onPhoneNumberClicked()
                }.disposedBy(disposeBag)

        buyTickets.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onBuyTicketClicked()
                }.disposedBy(disposeBag)

        searchIcon.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onClickSearch()
                }.disposedBy(disposeBag)

        requireActivity().title = resources.getString(R.string.museumInformation)

    }

    override fun setupNavigationBindings(viewModel: MuseumInformationViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .subscribe {
                    when (it) {
                        is Navigate.Forward -> {
                            when (it.endpoint) {
                                is MuseumInformationViewModel.NavigationEndpoint.BuyTicket -> {
                                    val url = (it.endpoint as MuseumInformationViewModel.NavigationEndpoint.BuyTicket).url
                                    customTabManager.openUrlOnChromeCustomTab(requireContext(), Uri.parse(url))
                                }
                                is MuseumInformationViewModel.NavigationEndpoint.CallMuseum -> {
                                    val phoneId = (it.endpoint as MuseumInformationViewModel.NavigationEndpoint.CallMuseum).phone
                                    val phone = getString(phoneId)
                                    val intent = Intent(Intent.ACTION_DIAL)
                                    intent.data = Uri.parse("tel:$phone")
                                    val chooser = Intent.createChooser(intent, resources.getString(R.string.callWith))
                                    startActivity(chooser)
                                }
                                is MuseumInformationViewModel.NavigationEndpoint.ShowMuseumInMap -> {
                                    val url = getString(R.string.museumGoogleMapSearchQuery)
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse(url)
                                    val chooser = Intent.createChooser(intent, resources.getString(R.string.viewMapWith))
                                    startActivity(chooser)
                                }
                                is MuseumInformationViewModel.NavigationEndpoint.Search -> {
                                    val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }
}
