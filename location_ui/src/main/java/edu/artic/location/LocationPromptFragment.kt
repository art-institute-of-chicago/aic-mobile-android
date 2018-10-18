package edu.artic.location

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenName
import edu.artic.location_ui.R
import edu.artic.map.overrideMapAccess
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_location_prompt.*
import kotlin.reflect.KClass


/**
 * One of the three primary `edu.artic.map.MapActivity` fragments, along
 * with `MapFragment` and `TutorialFragment`.
 */
class LocationPromptFragment : BaseViewModelFragment<LocationPromptViewModel>() {
    override val viewModelClass: KClass<LocationPromptViewModel> = LocationPromptViewModel::class
    override val title = R.string.noTitle
    override val layoutResId: Int = R.layout.fragment_location_prompt
    override val screenName: ScreenName? = null

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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is Activity) {
            // We expect this will be reset to 'auto' by 'TutorialFragment'.
            overrideMapAccess(context, View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
        }
    }
}