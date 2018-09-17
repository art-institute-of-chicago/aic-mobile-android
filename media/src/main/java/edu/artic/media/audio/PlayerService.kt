package edu.artic.media.audio

import edu.artic.db.Playable
import edu.artic.db.models.AudioFileModel

interface PlayerService {

    fun switchAudioTrack(alternative: AudioFileModel)

    fun pausePlayer()

    fun playPlayer(given: Playable?)

    fun playPlayer(audioFile: Playable, audioModel: AudioFileModel)

    fun resumePlayer()

    fun stopPlayer()
}