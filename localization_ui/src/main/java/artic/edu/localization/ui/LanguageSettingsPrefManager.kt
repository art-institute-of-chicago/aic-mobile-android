package artic.edu.localization.ui

import android.content.Context
import edu.artic.base.BasePreferencesManager

/**
 * @author Sameer Dhakal (Fuzz)
 */
class LanguageSettingsPrefManager(context: Context)
    : BasePreferencesManager(context, "language_settings") {

    var seenLanguageSettingsDialog: Boolean
        set(value) = putBoolean("saw_language_settings_dialog", value)
        get() = getBoolean("saw_language_settings_dialog")
}


