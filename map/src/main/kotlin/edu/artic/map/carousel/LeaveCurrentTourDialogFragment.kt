package edu.artic.map.carousel

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenName
import edu.artic.map.R
import edu.artic.ui.BaseFragment
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_leave_tour_dialog.*

/**
 * @author Sameer Dhakal (Fuzz)
 */
class LeaveCurrentTourDialogFragment : BaseFragment() {

    private var callback: LeaveTourCallback? = null

    fun attachTourStateListener(listener: LeaveTourCallback) {
        callback = listener
    }

    override val title = R.string.noTitle

    override val screenName: ScreenName?
        get() = null

    override val layoutResId: Int = R.layout.fragment_leave_tour_dialog

    override val overrideStatusBarColor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.LeaveTourDialogTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        leave.clicks()
                .defaultThrottle()
                .subscribeBy {
                    dismiss()
                    callback?.leftTour()
                }
                .disposedBy(disposeBag)

        stay.clicks()
                .defaultThrottle()
                .subscribeBy {
                    dismiss()
                    callback?.stayed()
                }
                .disposedBy(disposeBag)
    }

    interface LeaveTourCallback {
        fun leftTour()
        fun stayed()
    }

}