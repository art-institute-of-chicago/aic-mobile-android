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
 * an [AudioSelectFragment] or an [AudioDetailsFragment].
 *
 * Note that we do not use [NarrowAudioPlayerFragment][edu.artic.audioui.NarrowAudioPlayerFragment]
 * here at this time - that will be done soon as a prerequisite for ticket AIC-35.
 */
class AudioActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_audio

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
