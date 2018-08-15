package edu.artic.audio

import android.os.Bundle
import edu.artic.base.utils.disableShiftMode
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_audio.*

/**
 * One of the four primary sections of the app. This Activity may host either
 * an [AudioSelectFragment] or an [AudioDetailsFragment].
 *
 * Note that we do not use [BottomAudioPlayerFragment][edu.artic.audioui.BottomAudioPlayerFragment]
 * here at this time - that will be done soon as a prerequisite for ticket AIC-35.
 */
class AudioActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_audio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomNavigation.apply {
            disableShiftMode(R.color.audio_menu_color_list)
            selectedItemId = R.id.action_audio
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }
}
