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
    val playbackStartedForFirstTime: Subject<Boolean> = PublishSubject.create()
}


/**
 * @author Sameer Dhakal (Fuzz)
 */

class AudioPrefManager(context: Context)
    : BasePreferencesManager(context, "audio_playback") {

    var hasPlayedAudioBefore: Boolean
        set(value) = putBoolean("hasPlayedAudioBefore", value)
        get() = getBoolean("hasPlayedAudioBefore", false)
}