package edu.artic.util

import android.support.annotation.IdRes
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHost
import edu.artic.ui.BaseFragment

/**
 * The functionality of [edu.artic.media.ui.NarrowAudioPlayerFragment] depends on it finding
 * the correct host. This method simplifies that. See inline comments for implementation
 * details.
 *
 * **NB:** If other modules are found to benefit from this logic, please migrate the file to the
 * `ui` or `navigation` module. At this time it is only used in one place, and hence does not
 * belong there.
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
@UiThread
fun BaseFragment.findContainingNavController(@IdRes destOrNodeId: Int): NavController {
    val controllers: MutableList<NavController> = ArrayList()

    // First, collate all NavControllers for which
    //
    // 1. we're allowed to run a fragment transaction
    // and
    // 2. the associated ViewGroup container contains this fragment's view

    var ancestralFragment: Fragment? = parentFragment

    while (ancestralFragment != null) {
        if (ancestralFragment is NavHost) {
            controllers.add(ancestralFragment.navController)
        }
        ancestralFragment = ancestralFragment.parentFragment
    }

    activity?.let {
        if (it is NavHost) {
            controllers.add(it.navController)
        }
    }

    // Second, look for 'destOrNodeId' in each one. Max depth of 1 at this time.

    val bestController = controllers.firstOrNull { controller ->
        val capableDestinations = controller.graph
                .filter { destination ->
                    when (destination) {
                        is NavGraph -> {
                            when {
                                destination.findNode(destOrNodeId) != null -> true
                                destination.getAction(destOrNodeId) != null -> true
                                else -> false
                            }
                        }
                        else -> destination.id == destOrNodeId
                    }
                }

        return@firstOrNull capableDestinations.isNotEmpty()
    }

    return bestController!!
}