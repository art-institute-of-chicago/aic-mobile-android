package edu.artic.audio

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import edu.artic.base.utils.disableShiftMode
import edu.artic.media.ui.NarrowAudioPlayerFragment
import edu.artic.media.ui.NarrowAudioPlayerFragment.Companion.ARG_SKIP_TO_DETAILS
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_audio.*

/**
 * One of the four primary sections of the app. This Activity may host either
 * an [AudioLookupFragment] or an [AudioDetailsFragment].
 *
 * Note that a [NarrowAudioPlayerFragment] anywhere in the app may deep-link
 * to this screen. When it does so, we record that state in [willNavigate]
 * and perform the navigation in [onStart]. This prevents us from accidentally
 * showing [AudioLookupFragment] first.
 */
class AudioActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_audio

    /**
     * Lifecycle-check set by [onCreate] and consumed by [onStart].
     *
     * Always reset to false by [onDestroy].
     */
    private var willNavigate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras: Bundle = intent.extras ?: Bundle.EMPTY

        willNavigate = extras.getBoolean(ARG_SKIP_TO_DETAILS, false)

        bottomNavigation.apply {
            disableShiftMode(R.color.audio_menu_color_list)
            selectedItemId = R.id.action_audio
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }

    override fun onStart() {
        super.onStart()

        if (willNavigate) {
            willNavigate = false
            val navFragment = supportFragmentManager.primaryNavigationFragment
            if (navFragment is NavHostFragment) {
                navFragment.navController.navigate(R.id.see_current_audio_details)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        willNavigate = false
    }
}
