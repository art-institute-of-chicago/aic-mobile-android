package edu.artic.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.AudioAttributesCompat
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import edu.artic.base.utils.isServiceRunningInForeground

/**
 * @author Sameer Dhakal (Fuzz)
 */
class AudioPlayerService : Service() {

    object Constants {
        const val FOREGROUND_CHANNEL_ID = "foreground_channel_id"
    }

    private val binder: Binder = AudioPlayerServiceBinder()
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private var audioStreamUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        playerNotificationManager = PlayerNotificationManager(
                this,
                Constants.FOREGROUND_CHANNEL_ID,
                22,
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                        val notificationIntent = Intent(this@AudioPlayerService, AudioActivity::class.java)
                        return PendingIntent.getActivity(this@AudioPlayerService, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    }

                    override fun getCurrentContentText(player: Player?): String? {
                        val window = player?.currentWindowIndex
                        return "test title"
                    }

                    override fun getCurrentContentTitle(player: Player?): String {
                        val window = player?.currentWindowIndex
                        return "description"
                    }

                    override fun getCurrentLargeIcon(player: Player?, callback: PlayerNotificationManager.BitmapCallback?): Bitmap? {
                        return null
                    }

                })
        playerNotificationManager.setUseNavigationActions(false)
        playerNotificationManager.setStopAction(null)
        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setRewindIncrementMs(0)
        initializePlayer()
        playerNotificationManager.setPlayer(player)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun onPause() {
        player.playWhenReady = false
        stopForeground(false)
    }

    private fun onPlay() {
        player.playWhenReady = true
    }

    //TODO recheck logic
    private fun onStop() {
        if (!isServiceRunningInForeground(this, AudioPlayerService::class.java)) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        /**
         * Handles null condition.
         */
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            MusicConstants.ACTION.PAUSE_ACTION -> {
                onPause()
            }

            MusicConstants.ACTION.PLAY_ACTION -> {
                onPlay()
            }

            MusicConstants.ACTION.STOP_ACTION -> {
                onStop()
            }
            else -> {
                player.stop()
                stopForeground(true)
                stopSelf()
            }
        }

        return Service.START_NOT_STICKY
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    inner class AudioPlayerServiceBinder : Binder() {
        fun getService(): AudioPlayerService {
            return this@AudioPlayerService
        }
    }

    fun setAudioUrl(url: String, resetPosition: Boolean = false) {
        if (audioStreamUrl != url) {
            audioStreamUrl = url
            val uri = Uri.parse(url)
            val mediaSource = buildMediaSource(uri)
            player.prepare(mediaSource, resetPosition, false)
        }
    }

    private fun initializePlayer() {
        player.addListener(object : Player.DefaultEventListener() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    onPlay()
                } else if (playWhenReady) {
                    Log.d("music", "playing")
                } else {
                    onPause()
                }
            }
        })
    }

    private val audioAttributes = AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .build()

    val player: ExoPlayer by lazy {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        AudioFocusExoPlayerDecorator(audioAttributes,
                audioManager,
                ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this),
                        DefaultTrackSelector(),
                        DefaultLoadControl()))
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        return ExtractorMediaSource.Factory(
                DefaultHttpDataSourceFactory("exoplayer-aic")).createMediaSource(uri)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

}

