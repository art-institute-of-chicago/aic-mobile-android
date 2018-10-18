package edu.artic.localization.ui

import android.content.Context
import edu.artic.base.BasePreferencesManager

/**
 * @author Sameer Dhakal (Fuzz)
 */
class LanguageSettingsPrefManager(context: Context)
    : BasePreferencesManager(context, "language_settings") {

    var userSelectedLanguage: Boolean
        set(value) = putBoolean("user_selected_language", value)
        get() = getBoolean("user_selected_language")
}


