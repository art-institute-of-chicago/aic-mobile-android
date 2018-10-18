package edu.artic.map

import android.app.Activity
import android.view.ViewGroup
import edu.artic.location_ui.R

/**
 * Quick setter for the first `mapFragmentRoot` found in the given Activity's layout (if any).
 *
 * Use this to adjust [ViewGroup.setImportantForAccessibility].
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
fun overrideMapAccess(act: Activity?, mode: Int) {
    val root = act?.findViewById<ViewGroup?>(R.id.mapFragmentRoot)

    root?.importantForAccessibility = mode
}