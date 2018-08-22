package edu.artic.map

import android.os.Bundle
import edu.artic.base.utils.disableShiftMode
import edu.artic.db.models.ArticTour
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

        fun argsBundle(tour: ArticTour) = Bundle().apply {
            putParcelable(ARG_TOUR, tour)
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
}