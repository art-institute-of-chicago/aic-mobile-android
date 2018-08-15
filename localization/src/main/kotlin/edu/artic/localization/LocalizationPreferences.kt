package edu.artic.localization

import android.content.Context
import edu.artic.base.BasePreferencesManager

/**
 * Preferences re: what language to use.
 *
 * This would the place to store preferred measurement units and timezone.
 *
 * @author Piotr Leja (Fuzz)
 */
class LocalizationPreferences(context: Context)
    : BasePreferencesManager(context, "localization") {

    var applicationLanguage: String
        set(value) = putString("application_language", value)
        get() = getString("application_language", "en")!!

}
