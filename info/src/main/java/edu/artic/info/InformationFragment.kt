package edu.artic.info

import com.fuzz.rx.disposedBy
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_information.*
import kotlin.reflect.KClass


class InformationFragment : BaseViewModelFragment<InformationViewModel>() {

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
    }

    override fun setupNavigationBindings(viewModel: InformationViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .subscribe {
                    when(it) {
                        is Navigate.Forward -> {
                            when(it.endpoint) {
                                InformationViewModel.NavigationEndpoint.AccessMemberCard -> TODO()
                                InformationViewModel.NavigationEndpoint.Search -> {
                                    navController.navigate(R.id.goToSearch)
                                }
                            }
                        }
                    }
                }.disposedBy(disposeBag)
    }

}
