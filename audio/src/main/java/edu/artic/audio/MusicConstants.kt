package edu.artic.audio

/**
 * @author Sameer Dhakal (Fuzz)
 */

object MusicConstants {

    val NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503
    val DELAY_SHUTDOWN_FOREGROUND_SERVICE: Long = 20000
    val DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE: Long = 10000

    object ACTION {

        val MAIN_ACTION = "music.action.main"
        val PAUSE_ACTION = "music.action.pause"
        val PLAY_ACTION = "music.action.play"
        val START_ACTION = "music.action.start"
        val STOP_ACTION = "music.action.stop"

    }

    object STATE_SERVICE {

        val PREPARE = 30
        val PLAY = 20
        val PAUSE = 10
        val NOT_INIT = 0
    }

}

