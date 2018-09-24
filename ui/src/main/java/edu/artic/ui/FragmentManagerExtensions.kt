package edu.artic.ui

import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

/**
 * Use this to access the [androidx.navigation.NavController] hidden
 * within each of our [FragmentManager]s. Only works while the
 * activity's layout is inflated (i.e. between [BaseActivity.onCreate]
 * and [BaseActivity.onDestroy]).
 *
 * Analogous to [androidx.navigation.fragment.findNavController].
 */
fun FragmentManager.findNavController(): NavController? {
    val navFragment = primaryNavigationFragment
    return (navFragment as? NavHostFragment)?.navController
}

/**
 * Finds a fragment of type `T` within the hierarchy anchored at [fm] with id [fragmentId].
 *
 * Returns null if there is no such fragment.
 */
inline fun <reified T : BaseFragment> findFragmentInHierarchy(fm: FragmentManager, @IdRes fragmentId: Int): T? {
    var found: T? = null
    var potential: Fragment?
    var manager: FragmentManager? = fm
    do {
        potential = manager?.findFragmentById(fragmentId)
        if (potential is T) {
            found = potential
        }
        manager = manager?.primaryNavigationFragment?.childFragmentManager
    } while (found == null && manager != null)
    return found
}