package edu.artic.map

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.location.Location
import android.support.annotation.UiThread
import android.util.Log
import android.util.Property
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import edu.artic.base.utils.statusBarHeight
import edu.artic.db.models.ArticObject
import edu.artic.map.rendering.ALPHA_INVISIBLE
import edu.artic.map.rendering.ALPHA_VISIBLE


/**
 * Reference instance of [AlphaProperty]. Use this
 * to animate [Marker.setAlpha] and [Marker.getAlpha].
 *
 * @see [Marker.removeWithFadeOut]
 */
val MARKER_ALPHA: Property<Marker, Float> = AlphaProperty()

internal class AlphaProperty : Property<Marker, Float>(Float::class.java, "alpha") {
    override fun get(given: Marker?): Float {
        return given?.alpha ?: Float.NaN
    }

    override fun set(given: Marker?, value: Float) {
        given?.alpha = value
    }
}


/**
 * We only want to display [ArticObject] annotations that are within 15 meters
 * of the center of the map.
 *
 * @param bounds the restrictions of
 * [the map's viewport][com.google.android.gms.maps.Projection.getVisibleRegion]
 */
fun LatLng.isCloseEnoughToCenter(bounds: LatLngBounds): Boolean {
    return bounds.contains(this) && bounds.center.distanceTo(this) < 15
}

/**
 * Alias to [Location.distanceBetween], where 'this' is the first param and 'other' is the second.
 *
 * @return a distance, in meters
 */
fun LatLng.distanceTo(other: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
            this.latitude,
            this.longitude,
            other.latitude,
            other.longitude,
            results
    )
    return results[0]
}

/**
 * Google's Map API throws exceptions if a marker is used when either
 * 1. It is no longer attached to the map
 * 2. It has an invalid icon
 *
 * Situation 1 is vastly more likely; this is the result of issues
 * surrounding synchronization of access to the marker. This method
 * will print a warning when it detects those scenarios instead of
 * crashing the app.
 *
 * FIXME: Fix all such synchronization issues, then remove this method
 *
 * @return true if the operation failed, false if it succeeded
 */
@UiThread
fun <T> Marker.tryExpectingFailure(retry: Boolean = false, action: (Marker) -> T): Boolean {
    return try {
        action(this)
        false
    } catch (ex: IllegalArgumentException) {
        if (retry) {
            return tryExpectingFailure(false, action)
        } else {
            if (BuildConfig.DEBUG) {
                Log.w("MapMarker", ex.message)
            }
            true
        }
    }
}

/**
 * Fade this Marker from its current alpha/transparency value to [finalAlpha].
 *
 * May be paired with [removeWithFadeOut]. Always starts with
 * [the current value][MARKER_ALPHA], so for a complete fade-in effect you should set
 * that to [ALPHA_INVISIBLE] before calling this.
 */
@UiThread
fun Marker.fadeIn(finalAlpha: Float = ALPHA_VISIBLE) {
    val fadeIn: ObjectAnimator = ObjectAnimator.ofFloat(this, MARKER_ALPHA, alpha, finalAlpha)
    fadeIn.start()
}

/**
 * This calls [Marker.remove] after animating the [alpha][MARKER_ALPHA] to [ALPHA_INVISIBLE] -
 * essentially, fully fading it out. Counterpart to [fadeIn].
 *
 * Similar concept to [android.transition.Fade].
 */
@UiThread
fun Marker.removeWithFadeOut() {
    val fadeOut: ObjectAnimator = ObjectAnimator.ofFloat(this, MARKER_ALPHA, alpha, ALPHA_INVISIBLE)
    fadeOut.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            tryExpectingFailure(true) {
                remove()
            }
        }
    })
    fadeOut.start()
}

/**
 * Simple helper that allows us to include a [list] of [LatLng].
 */
fun LatLngBounds.Builder.includeAll(list: List<LatLng>) = apply { list.forEach { include(it) } }

/**
 * Specifies default padding. We add padding to the top so that StatusBar doesn't overlap the compass.
 * */
fun GoogleMap.setMapPadding(activity: Activity,
                            left: Int = 0,
                            top: Int = activity.statusBarHeight,
                            right: Int = 0,
                            bottom: Int = 0) {
    setPadding(left, top, right, bottom)
}