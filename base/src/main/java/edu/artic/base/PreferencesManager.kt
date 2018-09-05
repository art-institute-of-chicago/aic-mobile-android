package edu.artic.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/**
 * @author Sameer Dhakal (Fuzz)
 */
interface PreferencesManager {
    fun putLong(key: String, value: Long)

    fun putString(key: String, value: String?)

    fun putBoolean(key: String, value: Boolean)

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    fun getString(key: String, defaultValue: String? = ""): String?

    fun getLong(key: String, defaultValue: Long = 0): Long

    fun remove(key: String)
}

/**
 * Description: Provides some helper methods for calling [SharedPreferences].
 *
 * @author Andrew Grosner (Fuzz)
 */
open class BasePreferencesManager : PreferencesManager {

    private val sharedPreferences: SharedPreferences

    constructor(context: Context, fileName: String) {
        sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }

    constructor(sharedPreferences: SharedPreferences) {
        this.sharedPreferences = sharedPreferences
    }

    override fun putLong(key: String, value: Long) {
        edit().putLong(key, value).commit()
    }

    override fun putString(key: String, value: String?) {
        edit().putString(key, value).commit()
    }

    override fun putBoolean(key: String, value: Boolean) {
        edit().putBoolean(key, value).commit()
    }

    override fun remove(key: String) {
        edit().remove(key).commit()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = sharedPreferences.getBoolean(key, defaultValue)
    override fun getString(key: String, defaultValue: String?): String? = sharedPreferences.getString(key, defaultValue)
    override fun getLong(key: String, defaultValue: Long): Long = sharedPreferences.getLong(key, defaultValue)

    @SuppressLint("CommitPrefEdits")
    private fun edit(): SharedPreferences.Editor = sharedPreferences.edit()

}
