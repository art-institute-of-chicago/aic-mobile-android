package edu.artic.db

import android.content.Context
import edu.artic.base.BasePreferencesManager

class AppDataPreferencesManager(context: Context) : BasePreferencesManager(context, "appData") {

    var lastModified: String
        set(value) = putString("last_modified", value)
        get() = getString("last_modified", "").orEmpty()

    var downloadedNecessaryData: Boolean
        set(value) = putBoolean("last_modified", value)
        get() = getBoolean("last_modified", false)
}