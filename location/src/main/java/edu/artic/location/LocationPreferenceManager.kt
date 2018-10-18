package edu.artic.location

import android.content.Context
import edu.artic.base.BasePreferencesManager
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Preferences related to asking for permission to access
 * [system location info][android.location.LocationManager].
 *
 * Ui is handled by the ':location_ui' module.
 *
 * For the 'map tutorial' stuff, check the ':map' module
 * for `edu.artic.map.tutorial.TutorialPreferencesManager`.
 */
class LocationPreferenceManager(context: Context)
    : BasePreferencesManager(context, "location") {

    var hasRequestedPermissionOnce: Boolean
        set(value) = putBoolean("has_requested_permission_once", value)
        get() = getBoolean("has_requested_permission_once")

    var hasSeenLocationPrompt: Boolean
        set(value) {
            putBoolean("has_seen_location_prompt", value)
            hasSeenLocationPromptObservable.onNext(value)
        }
        get() = getBoolean("has_seen_location_prompt")

    var hasClosedLocationPromptOnce: Boolean
        set(value) {
            putBoolean("has_closed_location_prompt_once", value)
            hasClosedLocationPromptObservable.onNext(value)
        }
        get() = getBoolean("has_closed_location_prompt_once")

    val hasSeenLocationPromptObservable: Subject<Boolean> = BehaviorSubject.createDefault(hasSeenLocationPrompt)

    val hasClosedLocationPromptObservable: Subject<Boolean> = BehaviorSubject.createDefault(hasClosedLocationPromptOnce)
}