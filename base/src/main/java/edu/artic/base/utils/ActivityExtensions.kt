package edu.artic.base.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context

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

/**
 * 175 megabytes, expressed in bytes.
 *
 * This is a firm boundary for a rather vague concept; essentially,
 * we feel it is acceptable to ask for more resources if it probably
 * won't slow down the rest of the device. To determine the amount
 * of RAM that the app can reasonably request, we've (informally)
 * checked [Runtime.maxMemory] on a couple of real-world devices.
 *
 * On a 1GB device (such as the Moto G3) we can expect that number
 * to be around 100MB. On a 4GB device (such as the LG V30), the
 * number is more like 250MB. These are just hints, of course,
 * but we want to err on the side of caution when we can.
 */
internal const val MIN_MEMORY_FOR_HIGH_QUALITY : Long = 175 * 1_000_000

/**
 * Determine whether we need to limit memory and CPU usage.
 *
 * Note that we specifically want to avoid requesting the
 * [Large Heap flag][android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP] on
 * the app process. Refer to the official docs at
 * [https://developer.android.com/topic/performance/memory] for rationale.
 *
 * By default, the app assumes that there are no resource constraints, as
 * that is the same conclusion used by established helper methods like
 * [ActivityManagerCompat.isLowRamDevice][android.support.v4.app.ActivityManagerCompat.isLowRamDevice].
 *
 * We use 4 indicators at this time. If any flag up to us, this method returns true
 * and the remaining indicators are not checked. They are as follows:
 * 1. is this object [not allowed to access the filesystem][Context.isRestricted]?
 * 1. are we running on a [low-RAM device][ActivityManager.isLowRamDevice]?
 * 1. do we only have access to [one processor][Runtime.availableProcessors]?
 * 1. is the maximum memory available to our process under [175 megabytes][MIN_MEMORY_FOR_HIGH_QUALITY]?
 *
 * If you're wondering about that last check, the amount of RAM we'd prefer to
 * use is noticeably higher than AOSP's 'low RAM' threshold.
 */
fun Context.isResourceConstrained() : Boolean {
    val am = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?)

    val retVal =
            // Restricted Contexts can't cache stuff on the filesystem
            isRestricted or
            // Low RAM devices have under 1GB of RAM
            (am != null && am.isLowRamDevice) or
            // If there's only one CPU core available for us, we need to be lean
            (Runtime.getRuntime().availableProcessors() == 1) or
            // Our VM might not have high priority. Less than this much memory and performance might suffer
            (Runtime.getRuntime().maxMemory() < MIN_MEMORY_FOR_HIGH_QUALITY)

    return retVal
}


val Activity.statusBarHeight: Int
    get() {
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId != 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }