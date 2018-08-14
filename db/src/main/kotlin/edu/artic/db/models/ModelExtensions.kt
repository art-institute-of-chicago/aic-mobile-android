package edu.artic.db.models

/**
 * @author Sameer Dhakal (Fuzz)
 */

fun ArticObject.getAudio(): ArticAudioFile? {
    return if (this.audioCommentary.isNotEmpty()) {
        this.audioCommentary.first().audioFile
    } else {
        null
    }
}
