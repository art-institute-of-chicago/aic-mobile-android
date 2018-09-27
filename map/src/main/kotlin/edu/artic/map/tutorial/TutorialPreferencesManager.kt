package edu.artic.map.tutorial

import android.content.Context
import edu.artic.base.BasePreferencesManager
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber

class TutorialPreferencesManager(context: Context) : BasePreferencesManager(context, "tutorialPreferences") {

    var hasSeenTutorial: Boolean
        set(value) {
            putBoolean("has_seen_tutorial", value)
            hasSeenTutorialObservable.onNext(value)
        }
        get() = getBoolean("has_seen_tutorial", false)


    var hasClosedTutorialOnce: Boolean
        set(value) {
            Timber.d("newValueFor hasClosedTutorialOnce: $value")
            putBoolean("has_closed_tutorial", value)
            hasClosedTutorialObservable.onNext(value)
        }
        get() = getBoolean("has_closed_tutorial", false)

    val hasSeenTutorialObservable: Subject<Boolean> = BehaviorSubject.createDefault(hasSeenTutorial)

    val hasClosedTutorialObservable: Subject<Boolean> = BehaviorSubject.createDefault(hasClosedTutorialOnce)

}