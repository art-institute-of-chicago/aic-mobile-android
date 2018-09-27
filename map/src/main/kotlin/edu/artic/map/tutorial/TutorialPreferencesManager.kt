package edu.artic.map.tutorial

import android.content.Context
import edu.artic.base.BasePreferencesManager
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class TutorialPreferencesManager(context: Context) : BasePreferencesManager(context, "tutorialPreferences") {

    var hasSeenTutorial: Boolean
        set(value) {
            putBoolean("has_seen_tutorial", value)
            hasSeenTutorialObservable.onNext(value)
        }
        get() = getBoolean("has_seen_tutorial", false)

    val hasSeenTutorialObservable: Subject<Boolean> = BehaviorSubject.createDefault(hasSeenTutorial)

}