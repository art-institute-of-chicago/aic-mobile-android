package edu.artic.audio

import android.content.Intent
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import edu.artic.media.ui.NarrowAudioPlayerFragment
import edu.artic.viewmodel.BaseViewModel
import javax.inject.Inject


/**
 * Representation of excess [AudioActivity] logic.
 */
class AudioNavViewModel @Inject constructor() : BaseViewModel() {

    /**
     * Lifecycle-check set by [AudioActivity.onCreate] and consumed by [AudioActivity.onStart].
     *
     * Always reset to false by [AudioActivity.onDestroy].
     */
    private var willNavigate = false


    @UiThread
    fun detectDetailsScreenLink(intent: Intent) {
        val extras: Bundle = intent.extras ?: Bundle.EMPTY

        willNavigate = extras.getBoolean(NarrowAudioPlayerFragment.ARG_SKIP_TO_DETAILS, false)
    }


    @UiThread
    fun navigateToAudioDetailsScreen(fm: FragmentManager) {
        if (willNavigate) {
            willNavigate = false
            val navFragment = fm.primaryNavigationFragment
            if (navFragment is NavHostFragment) {
                navFragment.navController.navigate(R.id.see_current_audio_details)
            }
        }
    }

    @UiThread
    fun clearNavigationState() {
        willNavigate = false
    }
}
