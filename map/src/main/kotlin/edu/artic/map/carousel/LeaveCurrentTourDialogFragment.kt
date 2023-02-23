package edu.artic.map.carousel

import android.os.Bundle
import android.view.View
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenName
import edu.artic.map.R
import edu.artic.map.databinding.FragmentLeaveTourDialogBinding
import edu.artic.ui.BaseFragment
import io.reactivex.rxkotlin.subscribeBy

//import kotlinx.android.synthetic.main.fragment_leave_tour_dialog.*

/**
 * @author Sameer Dhakal (Fuzz)
 */
class LeaveCurrentTourDialogFragment : BaseFragment<FragmentLeaveTourDialogBinding>() {

    private var callback: LeaveTourCallback? = null

    fun attachTourStateListener(listener: LeaveTourCallback) {
        callback = listener
    }

    override val title = R.string.global_empty_string

    override val screenName: ScreenName?
        get() = null

    override val overrideStatusBarColor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_TITLE, R.style.LeaveTourDialogTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.leave.clicks()
            .defaultThrottle()
            .subscribeBy {
                dismiss()
                callback?.leftTour()
            }
            .disposedBy(disposeBag)

        binding.stay.clicks()
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