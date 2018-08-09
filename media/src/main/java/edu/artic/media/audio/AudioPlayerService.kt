package edu.artic.media.audio

import android.app.Notification
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
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.db.models.ArticObject
import edu.artic.media.R

/**
 * @author Sameer Dhakal (Fuzz)
 */
class AudioPlayerService : Service() {

    object Constants {
        const val FOREGROUND_CHANNEL_ID = "foreground_channel_id"
    }

    private val binder: Binder = AudioPlayerServiceBinder()
    private lateinit var playerNotificationManager: PlayerNotificationManager
    var articObject: ArticObject? = null

    /**
     * Returns the intent to load the details of currently playing audio file.
     */
    fun getIntent(): Intent {
        val audioIntent = "edu.artic.audio".asDeepLinkIntent()
        audioIntent.putExtra("artic_object", articObject)
        return audioIntent
    }

    override fun onCreate() {
        super.onCreate()
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                this,
                Constants.FOREGROUND_CHANNEL_ID,
                R.string.channel_name,
                22,
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                        val notificationIntent = "edu.artic.audio".asDeepLinkIntent()
                        return PendingIntent.getActivity(this@AudioPlayerService, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    }

                    override fun getCurrentContentText(player: Player?): String? {
                        return null
                    }

                    override fun getCurrentContentTitle(player: Player?): String {
                        return articObject?.title ?: ""
                    }

                    override fun getCurrentLargeIcon(player: Player?, callback: PlayerNotificationManager.BitmapCallback?): Bitmap? {
                        return null
                    }
                })

        playerNotificationManager.setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationCancelled(notificationId: Int) {
            }

            override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
                startForeground(notificationId, notification)
            }
        })

        playerNotificationManager.setUseNavigationActions(false)
        playerNotificationManager.setStopAction(null)
        playerNotificationManager.setFastForwardIncrementMs(10 * 1000)
        playerNotificationManager.setRewindIncrementMs(10 * 1000)
        playerNotificationManager.setOngoing(false)
        playerNotificationManager.setPlayer(player)
        playerNotificationManager.setSmallIcon(R.drawable.icn_notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    fun stopPlayerService() {
        player.seekTo(0)
        stopForeground(true)
        stopSelf()
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

    fun setArticObject(_articObject: ArticObject, resetPosition: Boolean = false) {
        if (articObject != _articObject) {
            articObject = _articObject
            val fileUrl = articObject?.audioCommentary?.first()?.audioFile?.fileUrl
            fileUrl?.let { url ->
                val uri = Uri.parse(url)
                val mediaSource = buildMediaSource(uri)
                player.prepare(mediaSource, resetPosition, false)
            }
        }
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
        playerNotificationManager.setPlayer(null)
        player.release()
    }

    fun getCurrentObject(): ArticObject? {
        return articObject
    }

    fun resumeCurrentAudio(requestedAudioObject: ArticObject) {
        if (articObject?.nid != requestedAudioObject.nid) {
            setArticObject(requestedAudioObject, true)
        }
        player.playWhenReady = true
    }

    fun pauseCurrentAudio() {
        player.playWhenReady = false
    }

}

