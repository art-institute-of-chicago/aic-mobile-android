package edu.artic.audio

import android.os.Bundle
import edu.artic.base.utils.disableShiftMode
import edu.artic.media.ui.NarrowAudioPlayerFragment
import edu.artic.navigation.NavigationSelectListener
import edu.artic.viewmodel.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_audio.*
import kotlin.reflect.KClass

/**
 * One of the four primary sections of the app. This Activity may host either
 * an [AudioLookupFragment] or an [AudioDetailsFragment].
 *
 * Note that a [NarrowAudioPlayerFragment] anywhere in the app may deep-link
 * to this screen. When it does so, we record that state in
 * [AudioNavViewModel.willNavigate] and perform the navigation in [onStart].
 * This prevents us from accidentally showing [AudioLookupFragment] first.
 */
class AudioActivity : BaseViewModelActivity<AudioNavViewModel>() {

    override val viewModelClass: KClass<AudioNavViewModel>
        get() = AudioNavViewModel::class

    override val layoutResId: Int
        get() = R.layout.activity_audio


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.detectDetailsScreenLink(intent)

        bottomNavigation.apply {
            disableShiftMode(R.color.audio_menu_color_list)
            selectedItemId = R.id.action_audio
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.navigateToAudioDetailsScreen(supportFragmentManager)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearNavigationState()
    }
}
