package edu.artic.db

import android.content.Context
import edu.artic.base.BasePreferencesManager

class AppDataPreferencesManager(context: Context) : BasePreferencesManager(context, "appData") {

    var lastModified: String
        set(value) = putString("last_modified", value)
        get() = getString("last_modified", "").orEmpty()
}