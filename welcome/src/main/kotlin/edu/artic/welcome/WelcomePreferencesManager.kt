package edu.artic.welcome

import android.content.Context
import edu.artic.base.BasePreferencesManager

/**
 * @author Sameer Dhakal (Fuzz)
 */

class WelcomePreferencesManager(context: Context)
    : BasePreferencesManager(context, "welcome") {

    var shouldPeekTourSummary: Boolean
        set(value) = putBoolean("should_peek_tour_summary", value)
        get() = getBoolean("should_peek_tour_summary", true)

}
