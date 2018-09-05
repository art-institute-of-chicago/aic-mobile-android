package edu.artic.map

import android.content.Intent
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.disableShiftMode
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_map.*

/**
 * One of the four primary sections of the app. This Activity always hosts a [MapFragment].
 *
 * As a primary section, this class always contains a
 * [NarrowAudioPlayerFragment][edu.artic.audioui.NarrowAudioPlayerFragment].
 */
class MapActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_map

    companion object {
        val ARG_TOUR = "ARG_TOUR"
        val ARG_FOCUSED_OBJECT = "ARG_FOCUSED_OBJECT_ID"

        fun getLaunchIntent(tour: ArticTour, articObject: ArticObject? = null): Intent {
            return NavigationConstants.MAP.asDeepLinkIntent().apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                putExtras(Bundle().apply {
                    putParcelable(ARG_TOUR, tour)
                    articObject?.let { putParcelable(ARG_FOCUSED_OBJECT, it) }
                })
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        bottomNavigation.apply {
            disableShiftMode(R.color.map_menu_color_list)
            selectedItemId = R.id.action_map
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

}