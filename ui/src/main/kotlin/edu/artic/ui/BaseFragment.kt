package edu.artic.ui

import android.graphics.Color
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.Toolbar
import android.view.*
import com.fuzz.rx.DisposeBag
import dagger.android.support.AndroidSupportInjection
import edu.artic.base.utils.setWindowFlag

abstract class BaseFragment : Fragment() {

    var toolbar: Toolbar? = null
    var collapsingToolbar: CollapsingToolbarLayout? = null

    protected abstract val title: String

    @get:LayoutRes
    protected abstract val layoutResId: Int

    val baseActivity: BaseActivity
        get() = activity as BaseActivity

    val disposeBag = DisposeBag()
    val navigationDisposeBag = DisposeBag()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    private fun requireView() = view
            ?: throw IllegalStateException("Fragment " + this + " view is not created yet.")

    override fun onResume() {
        super.onResume()
        requireView().post { updateToolbar(requireView()) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    protected open fun hasTransparentStatusBar(): Boolean = false

    protected open fun hasHomeAsUpEnabled(): Boolean = true

    private fun updateToolbar(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        if (toolbar != null) {
            baseActivity.setSupportActionBar(toolbar)
            baseActivity.supportActionBar?.apply {
                setDisplayShowTitleEnabled(true)
                setDisplayHomeAsUpEnabled(hasHomeAsUpEnabled())
                setDisplayShowHomeEnabled(true)
            }

            baseActivity.title = title
        }

        collapsingToolbar = view.findViewById(R.id.collapsingToolbar)
        collapsingToolbar?.let {
            val toolbarTextTypeFace = ResourcesCompat.getFont(requireContext(), R.font.ideal_sans_medium)
            it.setCollapsedTitleTypeface(toolbarTextTypeFace)
            it.setExpandedTitleTypeface(toolbarTextTypeFace)
        }

        if (hasTransparentStatusBar()) {
            activity?.setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
            activity?.window?.statusBarColor = Color.TRANSPARENT
        } else {
            activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        baseActivity.title = title
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposeBag.clear()
    }

    override fun onDestroy() {
        // fix for bug where viewmodel store is not cleared on 27.1.0, might be fixed later.
        val activity = activity
        if (activity != null
                && activity.isFinishing
                && !activity.isChangingConfigurations) {
            viewModelStore.clear()
        }
        super.onDestroy()
    }


}