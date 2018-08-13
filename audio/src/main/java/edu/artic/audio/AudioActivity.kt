package edu.artic.audio

import android.os.Bundle
import edu.artic.base.utils.disableShiftMode
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_audio.*

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
