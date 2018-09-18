package edu.artic.location

import android.content.Context
import edu.artic.base.BasePreferencesManager

class LocationPreferenceManager(context: Context)
    : BasePreferencesManager(context, "location") {

    var hasRequestedPermissionOnce : Boolean
    set(value) = putBoolean("has_requested_permission_once", value)
    get() = getBoolean("has_requested_permission_once")
}