package edu.artic.db

import android.content.Context
import edu.artic.base.BasePreferencesManager

class AppDataPreferencesManager(context: Context) : BasePreferencesManager(context, FILE_NAME) {
    companion object {
        private const val FILE_NAME = "appData"
        private const val LAST_MODIFIED_KEY = "last_modified"
        private const val DOWNLOADED_NECESSARY_DATA_KEY = "downloaded_necessary_data"
        private const val SHOW_STARTUP_VIDEO_KEY = "show_startup_video"
    }

    var lastModified: String
        set(value) = putString(LAST_MODIFIED_KEY, value)
        get() = getString(LAST_MODIFIED_KEY, "").orEmpty()

    var downloadedNecessaryData: Boolean
        set(value) = putBoolean(DOWNLOADED_NECESSARY_DATA_KEY, value)
        get() = getBoolean(DOWNLOADED_NECESSARY_DATA_KEY, false)

    var shouldShowVideo: Boolean
        set(value) = putBoolean(SHOW_STARTUP_VIDEO_KEY, value)
        get() = getBoolean(SHOW_STARTUP_VIDEO_KEY, true)
}