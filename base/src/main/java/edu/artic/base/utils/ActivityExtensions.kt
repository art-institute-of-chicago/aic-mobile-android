package edu.artic.base.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * @author Sameer Dhakal (Fuzz)
 */


fun Activity.setWindowFlag(bits: Int, on: Boolean) {
    val win = window
    val winParams = win.attributes
    if (on) {
        winParams.flags = winParams.flags or bits
    } else {
        winParams.flags = winParams.flags and bits.inv()
    }
    win.attributes = winParams
}

fun Activity.quitIntent(recipient: Class<*>): Intent {
    val intent = Intent(this, recipient)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    return intent
}