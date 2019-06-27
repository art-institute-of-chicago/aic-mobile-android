package edu.artic.media

import com.google.android.exoplayer2.ExoPlayer

/**
 * @author Sameer Dhakal (Fuzz)
 */

fun ExoPlayer.refreshPlayBackState() {
    val playBackState = this.playWhenReady

    // If playback is ongoing, pause then resume.
    if (playBackState) {
        this.playWhenReady = false
        this.playWhenReady = true
    }
}
