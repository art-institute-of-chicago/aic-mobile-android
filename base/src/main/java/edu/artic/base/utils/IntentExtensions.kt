package edu.artic.base.utils

import android.content.Intent
import android.os.Parcelable

/**
 * File contains the extension methods for [Intent]
 * @author Sameer Dhakal (Fuzz)
 */

/**
 * Returns the parcelable with given key from extras then remove it.
 * @param key : extras Key
 * @return [Parcelable]
 */
fun <T : Parcelable> Intent?.removeAndReturnParcelable(key: String): T? {
    return this?.let {
        val data: T? = it.getParcelableExtra(key)
        removeExtra(key)
        return data
    }
}

/**
 * Returns the String with given key from extras then remove it.
 * @param key : extras Key
 * @return [String]
 */
fun Intent?.removeAndReturnString(key: String): String? {
    return this?.let {
        val data: String? = it.getStringExtra(key)
        removeExtra(key)
        return data
    }
}