package edu.artic

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber


/**
 * Sends the timber logs to Crashlytics.
 * @author Sameer Dhakal (Fuzz)
 */

class CrashlyticsTree : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        FirebaseCrashlytics.getInstance().log("$tag: $message")

        if (t != null) {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
