package edu.artic.media.audio

import android.content.Context
import edu.artic.base.BasePreferencesManager
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class AudioServiceHook {

    /**
     * Event will be emitted when user attempts to play the audio translation for the first time.
     */
    val displayAudioTutorial: Subject<Boolean> = PublishSubject.create()
}


class AudioPrefManager(context: Context)
    : BasePreferencesManager(context, "audio_playback") {

    var hasSeenAudioTutorial: Boolean
        set(value) = putBoolean("hasSeenAudioTutorial", value)
        get() = getBoolean("hasSeenAudioTutorial", false)
}