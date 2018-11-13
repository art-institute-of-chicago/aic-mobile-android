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

        Crashlytics.log(message)

        if (t != null) {
            Crashlytics.logException(t)
        }
    }
}
