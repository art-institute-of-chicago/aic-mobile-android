package edu.artic.ui

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