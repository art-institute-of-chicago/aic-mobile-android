package edu.artic.info

import android.net.Uri
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.ui.findNavController
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


        viewModel.buildVersion
                .subscribe { versionName ->
                    versionInfo.text = getString(R.string.versionInfo, versionName)
                }
                .disposedBy(disposeBag)

    }

    override fun setupNavigationBindings(viewModel: InformationViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is Navigate.Forward -> {
                            when (it.endpoint) {
                                InformationViewModel.NavigationEndpoint.AccessMemberCard -> TODO()
                                InformationViewModel.NavigationEndpoint.MuseumInformation -> {
                                    navController.navigate(R.id.goToMuseumInformationFragment)
                                }
                                InformationViewModel.NavigationEndpoint.Search -> {
                                    navController.navigate(R.id.goToSearch)
                                }
                                is InformationViewModel.NavigationEndpoint.JoinNow -> {
                                    val url = (it.endpoint as InformationViewModel.NavigationEndpoint.JoinNow).url
                                    customTabManager.openUrlOnChromeCustomTab(requireContext(), Uri.parse(url))
                                }
                            }
                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }

}
