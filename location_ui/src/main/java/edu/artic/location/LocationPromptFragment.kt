package edu.artic.location

import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenCategoryName
import edu.artic.location_ui.R
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_location_prompt.*
import kotlin.reflect.KClass


class LocationPromptFragment : BaseViewModelFragment<LocationPromptViewModel>() {
    override val viewModelClass: KClass<LocationPromptViewModel> = LocationPromptViewModel::class
    override val title = R.string.noTitle
    override val layoutResId: Int = R.layout.fragment_location_prompt
    override val screenCategory: ScreenCategoryName? = null

    override fun setupBindings(viewModel: LocationPromptViewModel) {
        super.setupBindings(viewModel)
        promptNotNowButton
                .clicks()
                .subscribe {
                    viewModel.onClickNotNowButton()
                }
                .disposedBy(disposeBag)

        promptOkButton
                .clicks()
                .subscribe {
                    viewModel.onClickOk()
                }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: LocationPromptViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filter { it is Navigate.Back }
                .subscribe {
                    navController.popBackStack()
                }.disposedBy(navigationDisposeBag)
    }
}