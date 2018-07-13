package edu.artic.welcome

import android.content.Context
import edu.artic.base.BasePreferencesManager

/**
 * @author Sameer Dhakal (Fuzz)
 */

class WelcomePreferencesManager(context: Context)
    : BasePreferencesManager(context, "welcome") {

    var peekedTourSummary: Boolean
        set(value) = putBoolean("peeked_tour_summary", value)
        get() = getBoolean("peeked_tour_summary", false)

}
