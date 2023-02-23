package edu.artic.map

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import edu.artic.base.utils.disableShiftMode
import edu.artic.base.utils.preventReselection
import edu.artic.location.LocationPreferenceManager
import edu.artic.map.tutorial.TutorialPreferencesManager
import edu.artic.navigation.NavigationSelectListener
import edu.artic.navigation.linkHome
import edu.artic.ui.BaseActivity
//import kotlinx.android.synthetic.main.activity_map.*
import javax.inject.Inject

/**
 * One of the four primary sections of the app. This Activity always hosts a [MapFragment].
 *
 * On first load or under [certain circumstances][resetPrefs] it may host either a
 * [edu.artic.map.tutorial.TutorialFragment] or [edu.artic.location.LocationPromptFragment]
 * on top of the map layout.
 *
 * As a primary section, this class always contains a
 * [NarrowAudioPlayerFragment][edu.artic.media.ui.NarrowAudioPlayerFragment].
 */
class MapActivity : BaseActivity() {

    @Inject
    lateinit var locationPreferencesManager: LocationPreferenceManager
    @Inject
    lateinit var tutorialPreferencesManager: TutorialPreferencesManager

    override val layoutResId: Int
        get() = R.layout.activity_map

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bottomNavigation.apply {

            disableShiftMode(R.color.map_menu_color_list)
            selectedItemId = R.id.action_map

            preventReselection()

            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }

    override fun onStart() {
        super.onStart()
        // The map is resource-intensive. Be kind to it.
        Glide.get(this).setMemoryCategory(MemoryCategory.LOW)
    }

    override fun onStop() {
        super.onStop()
        // The GoogleMap itself will lower its usage at this point, so it is
        // safe to increase the memory category too.
        Glide.get(this).setMemoryCategory(MemoryCategory.NORMAL)
    }

    override fun onBackPressed() {
        if (isRootFragment(R.id.mapFragment)) {
            startActivity(linkHome())
            return
        }
        super.onBackPressed()
    }
}