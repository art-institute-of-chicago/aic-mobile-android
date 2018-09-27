package edu.artic.location

import android.os.Bundle
import android.view.View
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Do not allow any touch events to go below this view
        view.setOnTouchListener { _, _ -> true }
    }

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
                    activity?.onBackPressed()
                }.disposedBy(navigationDisposeBag)
    }
}