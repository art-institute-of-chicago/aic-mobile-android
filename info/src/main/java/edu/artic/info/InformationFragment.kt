package edu.artic.info

import android.net.Uri
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_information.*
import javax.inject.Inject
import kotlin.reflect.KClass


class InformationFragment : BaseViewModelFragment<InformationViewModel>() {

    @Inject
    lateinit var customTabManager: CustomTabManager

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Information

    override val viewModelClass: KClass<InformationViewModel>
        get() = InformationViewModel::class

    override val title: String
        get() = "Information"

    override fun hasTransparentStatusBar(): Boolean = true

    override val layoutResId: Int
        get() = R.layout.fragment_information

    override fun setupBindings(viewModel: InformationViewModel) {
        super.setupBindings(viewModel)
        appBarLayout.setOnSearchClickedConsumer(Consumer { viewModel.onClickSearch() })
        joinNow.clicks()
                .defaultThrottle()
                .subscribeBy {
                    viewModel.onClickJoinNow()
                }.disposedBy(disposeBag)

        museumInformation.clicks()
                .defaultThrottle()
                .subscribeBy {
                    viewModel.onMuseumInformationClicked()
                }.disposedBy(disposeBag)

        locationSettings.clicks()
                .defaultThrottle()
                .subscribeBy {
                    viewModel.onClickLocationSettings()
                }.disposedBy(disposeBag)


        viewModel.buildVersion
                .subscribe { versionName ->
                    versionInfo.text = getString(R.string.versionInfo, versionName)
                }
                .disposedBy(disposeBag)

        accessMemberCard.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onAccessMemberCardClicked()
                }
                .disposedBy(disposeBag)

    }

    override fun setupNavigationBindings(viewModel: InformationViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .observeOn(AndroidSchedulers.mainThread())
                .filterFlatMap({ it is Navigate.Forward }, { (it as Navigate.Forward).endpoint })
                .subscribe {
                    when (it) {
                        InformationViewModel.NavigationEndpoint.AccessMemberCard -> {
                            navController.navigate(R.id.goToAccessMemberCard)
                        }
                        InformationViewModel.NavigationEndpoint.MuseumInformation -> {
                            navController.navigate(R.id.goToMuseumInformationFragment)
                        }
                        InformationViewModel.NavigationEndpoint.Search -> {
                            navController.navigate(R.id.goToSearch)
                        }
                        is InformationViewModel.NavigationEndpoint.JoinNow -> {
                            val url = it.url
                            customTabManager.openUrlOnChromeCustomTab(requireContext(), Uri.parse(url))
                        }
                        InformationViewModel.NavigationEndpoint.LocationSettings -> {
                            navController.navigate(R.id.goToLocationSettings)
                        }
                    }

                }.disposedBy(navigationDisposeBag)
    }

}
