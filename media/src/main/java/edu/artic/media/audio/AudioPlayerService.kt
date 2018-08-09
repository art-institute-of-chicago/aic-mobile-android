package edu.artic.media.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.AudioManager.STREAM_VOICE_CALL
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.AudioAttributesCompat
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.db.models.ArticAudioFile
import edu.artic.db.models.ArticObject
import edu.artic.media.R
import io.reactivex.subjects.BehaviorSubject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class AudioPlayerService : Service() {

    object Constants {
        const val FOREGROUND_CHANNEL_ID = "foreground_channel_id"
        const val NOTIFICATION_ID = 200
    }

    sealed class PlayBackAction {
        class Play(val audioFile: ArticObject) : PlayBackAction()
        class Pause : PlayBackAction()
        class Resume : PlayBackAction()
        class Stop : PlayBackAction()
        class Seek(val time: Long) : PlayBackAction()
    }

    sealed class PlayBackState(val articAudioFile: ArticAudioFile) {
        class Playing(articAudioFile: ArticAudioFile) : PlayBackState(articAudioFile)
        class Paused(articAudioFile: ArticAudioFile) : PlayBackState(articAudioFile)
        class Stopped(articAudioFile: ArticAudioFile) : PlayBackState(articAudioFile)
    }

    private val binder: Binder = AudioPlayerServiceBinder()
    private lateinit var playerNotificationManager: PlayerNotificationManager
    var articObject: ArticObject? = null

    private val audioControl = BehaviorSubject.create<AudioPlayerService.PlayBackAction>()
    val audioPlayBackStatus = BehaviorSubject.create<AudioPlayerService.PlayBackState>()
    val currentTrack = BehaviorSubject.create<ArticAudioFile>()

    val disposeBag = DisposeBag()

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
        setUpNotificationManager()
        player.addListener(object : Player.DefaultEventListener() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                val articAudioFile = articObject?.audioCommentary?.first()?.audioFile
                articAudioFile?.let {
                    if (playWhenReady && playbackState == Player.STATE_READY) {
                        audioPlayBackStatus.onNext(PlayBackState.Playing(it))
                    } else if (playWhenReady) {
                        // might be idle (plays after prepare()),
                        // buffering (plays when data available)
                        // or ended (plays when seek away from end)
                    } else if (playbackState == Player.STATE_ENDED) {
                        audioPlayBackStatus.onNext(PlayBackState.Stopped(it))
                    } else {
                        audioPlayBackStatus.onNext(PlayBackState.Paused(it))
                    }
                }
            }
        })

        audioControl.subscribe {
            when (it) {
                is PlayBackAction.Play -> {
                    setArticObject(it.audioFile)
                    player.playWhenReady = true
                }

                is PlayBackAction.Resume -> {
                    player.playWhenReady = true
                }

                is PlayBackAction.Pause -> {
                    player.playWhenReady = false
                }

                is PlayBackAction.Stop -> {
                    val articAudioFile = articObject?.audioCommentary?.first()?.audioFile
                    articAudioFile?.let {
                        audioPlayBackStatus.onNext(PlayBackState.Stopped(it))
                    }
                    player.seekTo(0)
                    player.stop()
                }

                is PlayBackAction.Seek -> {
                    player.seekTo(it.time)
                }
            }
        }.disposedBy(disposeBag)
    }

    private fun setUpNotificationManager() {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                this,
                Constants.FOREGROUND_CHANNEL_ID,
                R.string.channel_name,
                Constants.NOTIFICATION_ID,
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                        //TODO make it dynamic so that activity that started the audio stream will be the destination of Intent
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
        if (articObject != _articObject || player.playbackState == Player.STATE_IDLE) {
            articObject = _articObject
            val audioFile = articObject?.audioCommentary?.first()?.audioFile
            audioFile?.let {
                val fileUrl = audioFile.fileUrl
                fileUrl?.let { url ->
                    val uri = Uri.parse(url)
                    val mediaSource = buildMediaSource(uri)
                    player.prepare(mediaSource, resetPosition, false)
                }
                currentTrack.onNext(audioFile)
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
                        DefaultLoadControl()).apply {
                    audioAttributes = AudioAttributes
                            .Builder()
                            .setFlags(STREAM_VOICE_CALL)
                            .build()
                })
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        return ExtractorMediaSource.Factory(
                DefaultHttpDataSourceFactory("exoplayer-aic")).createMediaSource(uri)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerNotificationManager.setPlayer(null)
        player.release()
        disposeBag.dispose()
    }

    fun pausePlayer() {
        audioControl.onNext(PlayBackAction.Pause())
    }

    fun playPlayer(audioFile: ArticObject?) {
        audioFile?.let {
            audioControl.onNext(PlayBackAction.Play(it))
        }
    }

    fun resumePlayer() {
        audioControl?.onNext(AudioPlayerService.PlayBackAction.Resume())
    }

    fun stopPlayer() {
        audioControl?.onNext(AudioPlayerService.PlayBackAction.Stop())
    }
}

