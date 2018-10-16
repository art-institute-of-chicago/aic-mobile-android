package edu.artic.map

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.disableShiftMode
import edu.artic.base.utils.preventReselection
import edu.artic.location.LocationPreferenceManager
import edu.artic.map.tutorial.TutorialPreferencesManager
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_map.*
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
            if (BuildConfig.DEBUG) {
                // Make sure we don't accidentally leak the activity.
                val ctx: Context = applicationContext
                setOnNavigationItemReselectedListener {
                    finish()
                    Handler().postDelayed({
                        resetPrefs()
                        Toast.makeText(ctx, "Map preference data erased.", Toast.LENGTH_SHORT).show()
                    }, 200L)
                }
            } else {
                preventReselection()
            }
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }

    /**
     * Internal, debug-only function. Wipe out all record of location and tutorial info,
     * then propagate `false` to all four convenience observables.
     */
    private fun resetPrefs() {
        locationPreferencesManager.clear()
        tutorialPreferencesManager.clear()
        locationPreferencesManager.hasSeenLocationPromptObservable.onNext(false)
        locationPreferencesManager.hasClosedLocationPromptObservable.onNext(false)
        tutorialPreferencesManager.hasSeenTutorialObservable.onNext(false)
        tutorialPreferencesManager.hasClosedTutorialObservable.onNext(false)
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
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (navController.currentDestination?.id == R.id.mapFragment) {
                val intent = NavigationConstants.HOME.asDeepLinkIntent()
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                startActivity(intent)
                return
            }
        }
        super.onBackPressed()
    }
}