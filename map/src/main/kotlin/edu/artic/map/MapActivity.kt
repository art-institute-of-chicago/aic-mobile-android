package edu.artic.map

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
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

    private val tour by lazy { intent?.extras?.getParcelable<ArticTour>(ARG_TOUR) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomNavigation.apply {
            disableShiftMode(R.color.map_menu_color_list)
            selectedItemId = R.id.action_map
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
        registerGraph(tour)
    }

    /**
     * Passing argument directly to startDestination is not possible without
     * building graph and setting the bundle manually.
     */
    private fun registerGraph(tour: ArticTour?) {
        val navHostFragment = container as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.map_navigation_graph)

        val bundle = Bundle().apply {
            tour?.let {
                /** using the argument key define in [R.navigation.map_navigation_graph] **/
                putParcelable("tour", tour)
            }
        }
        graph.addDefaultArguments(bundle)
        navHostFragment.navController.graph = graph
    }


}