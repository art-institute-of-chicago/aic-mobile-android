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
import android.support.v4.app.NotificationCompat
import android.support.v4.media.AudioAttributesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.STREAM_TYPE_VOICE_CALL
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.NotificationUtil
import com.google.android.exoplayer2.util.Util
import dagger.android.DaggerService
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.db.Playable
import edu.artic.db.models.*
import edu.artic.localization.LanguageSelector
import edu.artic.localization.nameOfLanguageForAnalytics
import edu.artic.media.R
import edu.artic.media.audio.AudioPlayerService.PlayBackAction
import edu.artic.media.audio.AudioPlayerService.PlayBackAction.*
import edu.artic.media.audio.AudioPlayerService.PlayBackState.*
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * The app's global background audio player.
 *
 * Most parts of the app will find it easier to work with the `NarrowAudioPlayerFragment`
 * wrapper class included in the `:audio_ui` module.
 *
 * The audio itself is played by [an ExoPlayer][player], and it controls a system notification
 * for changing/viewing the audio player's state via [PlayerNotificationManager].
 *
 * Audio files are streamed from URLs defined in the AppData blob. To play a track,
 * you'll need to submit a new ['Play' action][PlayBackAction.Play] to the [audioControl]
 * subject. You can pause or seek to a specific timestamp with [PlayBackAction.Pause] and
 * [PlayBackAction.Seek]. For convenience, we offer prebuilt [playPlayer] and [pausePlayer]
 * functions directly on the service.
 *
 * In general you should [bind to this service][android.app.Activity.bindService] in your
 * [BaseActivity's onResume() function][android.app.Activity.onResume] and [unbind from it]
 * [android.app.Activity.unbindService] in [onPause()][android.app.Activity.onPause].
 *
 * @author Sameer Dhakal (Fuzz)
 */
class AudioPlayerService : DaggerService(), PlayerService {

    companion object {
        val FOREGROUND_CHANNEL_ID = "foreground_channel_id"
        val NOTIFICATION_ID = 200

        fun getLaunchIntent(context: Context): Intent {
            return Intent(context, AudioPlayerService::class.java)
        }
    }

    val EMPTY_AUDIO_FILE = AudioFileModel("",null,null,null,null,null, null, null, null)

    /**
     * These are the inputs callers can use to change which [PlayBackState] should be
     * active. Currently supported actions are as follows:
     *
     * * [Play] - start playing a new track
     * * [Pause] - pause current track
     * * [Resume] - resumes current track
     * * [Stop] - stop current track
     * * [Seek] - change player position to a particular timestamp
     */
    sealed class PlayBackAction {
        /**
         * Play the track associated with the given [Playable].
         *
         * Currently, we only support tracks defined in [ArticObject.audioCommentary].
         *
         * @see [ExoPlayer.prepare]
         * @see [Player.setPlayWhenReady]
         */
        class Play(val audioFile: Playable, val audioModel: AudioFileModel) : PlayBackAction()

        /**
         * Pause the current track.
         *
         * You can continue where you left off with [Resume].
         *
         * @see [Player.setPlayWhenReady]
         */
        class Pause : PlayBackAction()

        /**
         * Resume the current track.
         *
         * You can also resume (without resetting the stream) by sending a subsequent [Play]
         * action backed by the same [ArticObject].
         *
         * @see [Player.setPlayWhenReady]
         */
        class Resume : PlayBackAction()

        /**
         * Stop playback, seek back to the start of the file, and move this service into the background.
         *
         * @see [Player.stop]
         */
        class Stop : PlayBackAction()

        /**
         * Move playback to a specific temporal position.
         *
         * @param time number of milliseconds from the start of the track
         */
        class Seek(val time: Long) : PlayBackAction()
    }

    /**
     * Current 'state' of [the ExoPlayer][player].
     *
     * We enforce the following states:
     * * [Playing]
     * * [Paused]
     * * [Stopped]
     *
     * To switch states, send a [PlayBackAction] to [audioControl].
     */
    sealed class PlayBackState(val audio: AudioFileModel) {
        class Playing(audio: AudioFileModel) : PlayBackState(audio)
        class Paused(audio: AudioFileModel) : PlayBackState(audio)
        class Stopped(audio: AudioFileModel) : PlayBackState(audio)
    }

    private val binder: Binder = AudioPlayerServiceBinder()
    private lateinit var playerNotificationManager: PlayerNotificationManager

    // NB: As this is an Android Service, we _CANNOT_ define '@Inject' properties in the constructor

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var languageSelector: LanguageSelector

    @Inject
    lateinit var audioServiceHook: AudioServiceHook

    @Inject
    lateinit var audioPrefManager: AudioPrefManager

    /**
     * Something with one or more audio tracks. See [currentTrack] and [AudioFileModel].
     */
    var playable: Playable? = null
        private set

    private val audioControl: Subject<PlayBackAction> = BehaviorSubject.create()
    val audioPlayBackStatus: Subject<PlayBackState> = BehaviorSubject.create()
    val currentTrack: Subject<AudioFileModel> = BehaviorSubject.create()

    val disposeBag = DisposeBag()
    internal var currentBitmap: Bitmap? = null


    override fun onCreate() {
        super.onCreate()
        setUpNotificationManager()
        player.addListener(object : Player.DefaultEventListener() {

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                (currentTrack as BehaviorSubject).value?.let { given ->
                    when {
                        playWhenReady && playbackState == Player.STATE_READY -> {
                            audioPlayBackStatus
                                    .take(1)
                                    .subscribe{
                                        when(it) {
                                            is Playing -> audioPlayBackStatus.onNext(PlayBackState.Paused(given))
                                            else -> audioPlayBackStatus.onNext(PlayBackState.Playing(given))
                                        }
                                    }.disposedBy(disposeBag)
                        }
                        playbackState == Player.STATE_ENDED -> {
                            /*Play back completed*/
                            analyticsTracker.reportEvent(EventCategoryName.PlayBack, AnalyticsAction.playbackCompleted, currentTrack.value?.title.orEmpty())
                            audioPlayBackStatus.onNext(PlayBackState.Stopped(given))
                            playerNotificationManager.setPlayer(null)
                            currentTrack.onNext(EMPTY_AUDIO_FILE)
                        }
                        playbackState == Player.STATE_IDLE -> audioPlayBackStatus.onNext(PlayBackState.Stopped(given))
                        else -> audioPlayBackStatus.onNext(PlayBackState.Paused(given))
                    }
                }
            }
        })

        audioControl.subscribe { playBackAction ->
            when (playBackAction) {
                is PlayBackAction.Play -> {
                    playerNotificationManager.setPlayer(player)
                    // No need to seek here; that'll be done in 'setArticObject' if needed
                    setArticObject(playBackAction.audioFile, playBackAction.audioModel)
                    /**
                     * If the audio is being played for the first time, make sure we invoke
                     * [AudioServiceHook.displayAudioTutorial] event.
                     * This hook is used for displaying the Audio Tutorial Screen.
                     */
                    if (!audioPrefManager.hasSeenAudioTutorial) {
                        audioServiceHook.displayAudioTutorial.onNext(true)
                    } else {
                        player.playWhenReady = true
                    }
                }

                is PlayBackAction.Resume -> {
                    player.playWhenReady = true
                }

                is PlayBackAction.Pause -> {
                    player.playWhenReady = false
                }

                is PlayBackAction.Stop -> {
                    (currentTrack as BehaviorSubject).value?.let { audioFile ->
                        audioPlayBackStatus.onNext(PlayBackState.Stopped(audioFile))
                    }
                    playerNotificationManager.setPlayer(null)
                    player.stop()
                }

                is PlayBackAction.Seek -> {
                    player.seekTo(playBackAction.time)
                }
            }
        }.disposedBy(disposeBag)
    }

    private fun setUpNotificationManager() {
        NotificationUtil.createNotificationChannel(
                this, FOREGROUND_CHANNEL_ID, R.string.channel_name, NotificationUtil.IMPORTANCE_LOW)
        playerNotificationManager = PlayerNotificationManager(
                this,
                FOREGROUND_CHANNEL_ID,
                NOTIFICATION_ID,
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
                        return playable?.getPlayableTitle().orEmpty()
                    }


                    override fun getCurrentLargeIcon(player: Player?, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                        if (currentBitmap == null) {
                            playable?.getPlayableThumbnailUrl()?.let {
                                Glide.with(this@AudioPlayerService)
                                        .asBitmap()
                                        .load(it)
                                        .into(BitmapCallbackTarget(this@AudioPlayerService, callback))
                            }
                        }
                        return currentBitmap
                    }
                },
                object : PlayerNotificationManager.CustomActionReceiver {
                    override fun getCustomActions(player: Player?): MutableList<String> {
                        return mutableListOf("Cancel_Notification")
                    }

                    override fun createCustomActions(context: Context?): MutableMap<String, NotificationCompat.Action> {
                        val playIntent = Intent("Cancel_Notification").setPackage(context?.packageName)
                        return mutableMapOf("Cancel_Notification" to NotificationCompat.Action(
                                R.drawable.ic_close_circle,
                                "Close",
                                PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT)))
                    }

                    override fun onCustomAction(player: Player?, action: String?, intent: Intent?) {
                        stopPlayer()
                    }
                })

        playerNotificationManager.setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationCancelled(notificationId: Int) {
                stopForeground(true)
            }

            override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
                startForeground(notificationId, notification)
            }
        })
        playerNotificationManager.apply {
            setUseNavigationActions(false)
            setStopAction(null)
            setFastForwardIncrementMs(10 * 1000)
            setRewindIncrementMs(10 * 1000)
            setOngoing(false)
            setPlayer(player)
            setSmallIcon(R.drawable.icn_notification)
        }

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

    fun setArticObject(_articObject: Playable, audio: AudioFileModel, resetPosition: Boolean = false) {

        val isDifferentAudio = (currentTrack as BehaviorSubject).value != audio

        if (isDifferentAudio) {
            analyticsTracker.reportEvent(
                    EventCategoryName.LanguageAudio,
                    audio.underlyingLocale().nameOfLanguageForAnalytics(),
                    audio.title.orEmpty()
            )
        }

        if (playable != _articObject || isDifferentAudio || player.playbackState == Player.STATE_IDLE) {
            currentBitmap = null

            /** Check if the current audio is being interrupted by other audio object.**/
            playable?.let { articObject ->
                if (player.playbackState != Player.STATE_IDLE) {
                    analyticsTracker.reportEvent(EventCategoryName.PlayBack, AnalyticsAction.playbackInterrupted, articObject.getPlayableTitle().orEmpty())
                }
            }
            playable = _articObject

            audio.let {
                audio.fileUrl?.let { url ->
                    val uri = Uri.parse(url)
                    val mediaSource = buildMediaSource(uri)
                    player.prepare(mediaSource, resetPosition, false)
                    player.seekTo(0)
                }
                currentTrack.onNext(audio)
            }
        }
    }

    /**
     * AIC wants to play the music through the ear piece.
     * @see SimpleExoPlayer.setAudioStreamType
     */
    private val audioAttributes = AudioAttributesCompat.Builder()
            .setContentType(Util.getAudioContentTypeForStreamType(STREAM_TYPE_VOICE_CALL))
            .setUsage(Util.getAudioUsageForStreamType(STREAM_TYPE_VOICE_CALL))
            .build()

    val player: ExoPlayer by lazy {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = false
        AudioFocusExoPlayerDecorator(audioAttributes,
                audioManager,
                ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this),
                        DefaultTrackSelector(),
                        DefaultLoadControl()).apply {
                    audioAttributes = AudioAttributes.Builder()
                            .setUsage(Util.getAudioUsageForStreamType(STREAM_TYPE_VOICE_CALL))
                            .setContentType(Util.getAudioContentTypeForStreamType(STREAM_TYPE_VOICE_CALL))
                            .build()
                }
        )
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

    /**
     * Counterpart to [switchAudioTrack]. When this returns true
     *
     * 1. [playable] is null
     * 2. we expect (but do not guarantee) that [player] is in
     * the [idle state][Player.STATE_IDLE]
     */
    fun hasNoTrack(): Boolean {
        return !(currentTrack as BehaviorSubject).hasValue()
    }

    /**
     * Pause current track, switch audio file, resume the new track at
     * that same position.
     *
     * If nothing is [currently playing][audioPlayBackStatus], this skips
     * the 'pause' and 'resume' operations.
     *
     * @see setArticObject
     */
    override fun switchAudioTrack(alternative: AudioFileModel) {
        playable?.let {
            val playBackState = (audioPlayBackStatus as BehaviorSubject).value
            if (playBackState is Playing) {
                pausePlayer()
                playPlayer(it, alternative)
            } else {
                playPlayer(it, alternative)
                pausePlayer()
            }
        }
    }

    override fun pausePlayer() {
        audioControl.onNext(PlayBackAction.Pause())
    }

    override fun playPlayer(given: Playable?) {
        if (given != null) {
            var audioFile: ArticAudioFile? = null
            when (given) {
                is ArticObject -> audioFile = given.audioFile
                is ArticTour -> audioFile = null
            }
            if (audioFile != null) {
                playPlayer(given, audioFile.preferredLanguage(languageSelector))
            }
        }
    }

    override fun playPlayer(audioFile: Playable, audioModel: AudioFileModel) {
        audioControl.onNext(PlayBackAction.Play(audioFile, audioModel))
    }

    override fun resumePlayer() {
        audioControl.onNext(AudioPlayerService.PlayBackAction.Resume())
    }

    override fun stopPlayer() {
        analyticsTracker.reportEvent(EventCategoryName.PlayBack, AnalyticsAction.playbackInterrupted, (currentTrack as BehaviorSubject).value?.title.orEmpty())
        audioControl.onNext(AudioPlayerService.PlayBackAction.Stop())
    }
}

/**
 * Kotlin(version:1.2.51) was unable to resolve this class when it was defined anonymously,
 * so had to create this class.
 */
class BitmapCallbackTarget(private val service: AudioPlayerService, private val callback: PlayerNotificationManager.BitmapCallback) : SimpleTarget<Bitmap>() {
    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
        callback.onBitmap(resource)
        service.currentBitmap = resource
    }
}