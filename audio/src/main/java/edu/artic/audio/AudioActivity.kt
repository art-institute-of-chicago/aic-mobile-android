package edu.artic.audio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.annotation.UiThread
import android.support.v4.app.FragmentManager
import android.view.View
import edu.artic.base.utils.disableShiftMode
import edu.artic.media.ui.NarrowAudioPlayerFragment
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import edu.artic.ui.findNavController
import kotlinx.android.synthetic.main.activity_audio.*

/**
 * One of the four primary sections of the app. This Activity may host either
 * an [AudioLookupFragment] or an [AudioDetailsFragment].
 *
 * Note that a [NarrowAudioPlayerFragment] anywhere in the app may deep-link
 * to this screen. When it does so, we record that state in
 * [AudioActivity.willNavigate] and perform the navigation in [onStart].
 * This prevents us from accidentally showing [AudioLookupFragment] first.
 */
class AudioActivity : BaseActivity() {

    companion object {
        /**
         * The number of milliseconds in two frames on a 60Hz refresh-rate
         * display, rounded to nearest whole number.
         */
        @Suppress("PrivatePropertyName")
        private const val TWO_FRAMES = 33L
    }

    override val layoutResId: Int
        get() = R.layout.activity_audio

    /**
     * Lifecycle-check set by [AudioActivity.onCreate] and consumed by [AudioActivity.onStart].
     *
     * Always reset to false by [AudioActivity.onDestroy].
     */
    private var willNavigate = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        detectDetailsScreenLink(intent)

        bottomNavigation.apply {
            disableShiftMode(R.color.audio_menu_color_list)
            selectedItemId = R.id.action_audio
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }

    @UiThread
    private fun detectDetailsScreenLink(intent: Intent) {
        val extras: Bundle = intent.extras ?: Bundle.EMPTY

        willNavigate = extras.getBoolean(NarrowAudioPlayerFragment.ARG_SKIP_TO_DETAILS, false)
    }

    override fun onStart() {
        super.onStart()

        // Unfortunately, the layout used by AudioActivity needs to be acclimated to the new content.
        Handler().postDelayed(
                {
                    // This ensures that all of the measurement calculations are locked in ASAP, before
                    // we display on screen OR any (likely to force relayout) touch events are registered.
                    findViewById<View>(android.R.id.content).forceLayout()
                },
                // Practical experimentation has found that one frame at 60fps (16ms) might execute
                // before the AudioLookupFragment content fully settles. The addition of just one more
                // frame fully belays that concern.
                TWO_FRAMES
        )

        navigateToAudioDetailsScreen(supportFragmentManager)
    }

    @UiThread
    private fun navigateToAudioDetailsScreen(fm: FragmentManager) {
        if (willNavigate) {
            willNavigate = false
            fm.findNavController()?.navigate(R.id.see_current_audio_details)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        willNavigate = false
    }
}
