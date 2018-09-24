package edu.artic.ui

import android.graphics.Color
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.annotation.UiThread
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.Toolbar
import android.view.*
import androidx.navigation.Navigation
import com.fuzz.rx.DisposeBag
import dagger.android.support.AndroidSupportInjection
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.getThemeColors
import edu.artic.base.utils.setWindowFlag
import timber.log.Timber
import javax.inject.Inject

abstract class BaseFragment : DialogFragment(), OnBackPressedListener {

    var toolbar: Toolbar? = null
    var collapsingToolbar: CollapsingToolbarLayout? = null

    @get:StringRes
    protected abstract val title: Int

    private var requestedTitle: String? = null

    fun requestTitleUpdate(proposedTitle: String) {
        this.requestedTitle = proposedTitle
        baseActivity.title = proposedTitle

        /**
         * BaseActivity.title does not update the title for the fragments with collapsing toolbar title.
         */

        collapsingToolbar?.title = proposedTitle
    }

    private fun getToolbarTitle(): String {
        return requestedTitle ?: getString(title)
    }

    @get:LayoutRes
    protected abstract val layoutResId: Int

    abstract val screenCategory: ScreenCategoryName?

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    val baseActivity: BaseActivity
        get() = activity as BaseActivity

    /**
     * # Lifecycle: `init{}` -> [onDestroyView]
     *
     * Do not add navigation observers to this; those belong in [navigationDisposeBag].
     */
    val disposeBag = DisposeBag()
    /**
     * # Lifecycle: [onResume] -> [onPause]
     */
    val navigationDisposeBag = DisposeBag()

    protected fun requireView() = view
            ?: throw IllegalStateException("Fragment " + this + " view is not created yet.")

    protected val navController
        get() = Navigation.findNavController(requireView())

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        screenCategory?.let {
            analyticsTracker.reportScreenView(it)
        }
    }

    override fun onResume() {
        super.onResume()
        // We can wait to update the toolbar text...
        requireView().post {
            view?.let {
                updateToolbar(it)
            }
        }
        // ...but we need to set status bar color RIGHT NOW (c.f. 'updateWindowProperties' docs)
        updateWindowProperties()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    protected open fun hasTransparentStatusBar(): Boolean = false

    protected open val customToolbarColorResource: Int
        get() = 0

    /**
     * If it is set, the status bar is painted with statusBarColor.
     */
    protected open val overrideStatusBarColor: Boolean
        get() = true

    protected open fun hasHomeAsUpEnabled(): Boolean = true

    @UiThread
    private fun updateToolbar(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        if (toolbar != null) {
            baseActivity.setSupportActionBar(toolbar)
            baseActivity.supportActionBar?.apply {
                setDisplayShowTitleEnabled(true)
                setDisplayHomeAsUpEnabled(hasHomeAsUpEnabled())
                setDisplayShowHomeEnabled(true)
            }

            baseActivity.title = getString(title)
        }

        collapsingToolbar = view.findViewById(R.id.collapsingToolbar)
        collapsingToolbar?.apply {
            val toolbarTextTypeFace = ResourcesCompat.getFont(requireContext(), R.font.ideal_sans_medium)
            setCollapsedTitleTypeface(toolbarTextTypeFace)
            setExpandedTitleTypeface(toolbarTextTypeFace)
        }

    }

    /**
     * # Usage
     *
     * Call this at any point after [onCreate] to change the appearance of the host's
     * [status bar][Window.setStatusBarColor].
     *
     * Does nothing if [overrideStatusBarColor] returns false.
     *
     * # Why is the fragment drawing partially _under_ the nav buttons?
     *
     * Simple answer: because you [posted][View.post] this method call instead of calling
     * inline in [BaseActivity.onResumeFragments] or [onResume].
     *
     * # But it's only doing that sometimes. What's wrong with posting the method call?
     *
     * If this is called on _just_ the right frame after [onResume] returns, our host's
     * DecorView might miscalculate the window inset. You can see this easily in the
     * SDK 26 emulator image by [posting a call][getView] to this method in [onResume]
     * and then rotating the device.
     *
     * I wasn't able to track down the precise cause, but it seems that it's strongly
     * related to the inset calculations in
     *
     * `com.android.internal.policy.DecorView`'s `updateColorViews` function.
     *
     *
     * The insets are set much earlier, when [BaseActivity.setContentView] actually
     * instantiates the `DecorView`. The call to `updateColorViews` triggered here
     * by [assigning statusBarColor][Window.setStatusBarColor] passes along a null
     * `WindowInsets` object, which means that cached inset values are used for layout
     * instead of new ones.
     *
     *
     * The only way I found to recompute the insets once you reach that state is to
     * call [view.getRootView()][View.getRootView] [.forceLayout()][View.forceLayout]
     * on the following frame. Of course, you can sidestep the whole issue by calling
     * this sufficiently early in the fragment lifecycle. In [onResume], for example.
     */
    @UiThread
    private fun updateWindowProperties() {
        if (overrideStatusBarColor) {
            // Since this is always run on UI thread, the activity and context won't
            // change for the duration of this call.
            val act = requireActivity()
            val ctx = requireContext()

            if (hasTransparentStatusBar()) {
                act.setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
                act.window?.statusBarColor = Color.TRANSPARENT
            } else {
                if (customToolbarColorResource == 0) {
                    act.setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
                    val primaryDarkColor = intArrayOf(android.support.design.R.attr.colorPrimaryDark)
                    ctx.getThemeColors(primaryDarkColor).getOrNull(0)?.defaultColor?.let {
                        act.window?.statusBarColor = it
                    }
                } else {
                    act.window?.statusBarColor = ContextCompat.getColor(ctx, customToolbarColorResource)
                }
            }
        }
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

    override fun onBackPressed(): Boolean {
        return false
    }

}

interface OnBackPressedListener {
    fun onBackPressed(): Boolean
}
