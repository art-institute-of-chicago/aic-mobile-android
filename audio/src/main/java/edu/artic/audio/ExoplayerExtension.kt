package edu.artic.audio

import com.google.android.exoplayer2.ExoPlayer

/**
 * @author Sameer Dhakal (Fuzz)
 */

fun ExoPlayer.refreshPlayBackState() {
    val playBackState = this.playWhenReady
    this.playWhenReady = false
    this.playWhenReady = playBackState
}
