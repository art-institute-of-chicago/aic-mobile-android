package edu.artic

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber


/**
 * Sends the timber logs to Crashlytics.
 * @author Sameer Dhakal (Fuzz)
 */

class CrashlyticsTree : Timber.Tree() {

    private val reportableLogs: Set<Int> = setOf(Log.ERROR)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (reportableLogs.contains(priority)) {
            return
        }

        Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority)
        Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag)
        Crashlytics.setString(CRASHLYTICS_KEY_MESSAGE, message)

        Crashlytics.log(message)

        if (t != null) {
            Crashlytics.logException(t)
        }
    }

    companion object {
        private const val CRASHLYTICS_KEY_PRIORITY = "priority"
        private const val CRASHLYTICS_KEY_TAG = "tag"
        private const val CRASHLYTICS_KEY_MESSAGE = "message"
    }
}
