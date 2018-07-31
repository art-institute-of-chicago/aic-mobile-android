package edu.artic.localizer

import android.content.Context
import edu.artic.base.BasePreferencesManager

/**
 * @author Sameer Dhakal (Fuzz)
 */

class LocalizerPreferences(context: Context)
    : BasePreferencesManager(context, "localizer") {

    var applicationLanguage: String
        set(value) = putString("application_language", value)
        get() = getString("application_language", "en")!!

}
