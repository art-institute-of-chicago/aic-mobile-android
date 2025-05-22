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
import android.os.Build
import android.os.IBinder
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.USAGE_VOICE_COMMUNICATION
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.NotificationUtil
import dagger.android.DaggerService
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.db.Playable
import edu.artic.db.models.*
import edu.artic.localization.LanguageSelector
import edu.artic.localization.nameOfLanguageForAnalytics
import edu.artic.media.audio.AudioPlayerService.PlayBackAction
import edu.artic.media.audio.AudioPlayerService.PlayBackAction.*
import edu.artic.media.audio.AudioPlayerService.PlayBackState.*
import edu.artic.tours.manager.TourProgressManager
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
        const val FOREGROUND_CHANNEL_ID = "foreground_channel_id"
        const val NOTIFICATION_ID = 200

        const val CANCEL_ACTION = "Cancel_Notification"

        fun getLaunchIntent(context: Context): Intent {
            return Intent(context, AudioPlayerService::class.java)
        }
    }

    val EMPTY_AUDIO_FILE = AudioFileModel("", null, null, null, null, null, null, null, null)

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

    @Inject
    lateinit var tourProgressManager: TourProgressManager

    /**
     * Something with one or more audio tracks. See [currentTrack] and [AudioFileModel].
     */
    var playable: Playable? = null
        private set

    private val audioControl: Subject<PlayBackAction> = BehaviorSubject.create()
    val audioPlayBackStatus: Subject<PlayBackState> = BehaviorSubject.create()
    val currentTrack: Subject<AudioFileModel> = BehaviorSubject.create()

    /**
     * Lifetime: [onCreate] to [onDestroy].
     *
     * Do not use after [onDestroy] or before [onCreate] (it's easy to accidentally do that
     * if the service is being recreated).
     */
    lateinit var disposeBag: DisposeBag
    internal var currentBitmap: Bitmap? = null

    /**
     * AIC wants to play the music through the ear piece.
     * @see SimpleExoPlayer.setAudioStreamType
     */

    lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(DefaultRenderersFactory(this))
            .setTrackSelector(DefaultTrackSelector(this))
            .setLoadControl(DefaultLoadControl())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(USAGE_VOICE_COMMUNICATION)
                    .build(),
                /* handleAudioFocus = */ false
            )
            .build()

        // Make sure to clear this out in ::onDestroy.
        disposeBag = DisposeBag()
        setUpNotificationManager()
        player.addListener(object : Player.Listener {

            @Deprecated("Deprecated in Java")
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                (currentTrack as BehaviorSubject).value?.let { given ->
                    when {
                        playWhenReady && playbackState == Player.STATE_READY -> {
                            audioPlayBackStatus
                                .take(1)
                                .subscribe {
                                    when (it) {
                                        is Playing -> audioPlayBackStatus.onNext(
                                            PlayBackState.Paused(
                                                given
                                            )
                                        )
                                        else -> audioPlayBackStatus.onNext(
                                            PlayBackState.Playing(
                                                given
                                            )
                                        )
                                    }
                                }.disposedBy(disposeBag)
                        }
                        playbackState == Player.STATE_ENDED -> {
                            /*Play back completed*/
                            analyticsTracker.reportEvent(
                                EventCategoryName.PlayBack,
                                AnalyticsAction.playbackCompleted,
                                currentTrack.value?.title.orEmpty()
                            )
                            audioPlayBackStatus.onNext(Stopped(given))
                            playerNotificationManager.setPlayer(null)
                            tourProgressManager.playBackEnded(given)
                            currentTrack.onNext(EMPTY_AUDIO_FILE)
                        }
                        playbackState == Player.STATE_IDLE -> audioPlayBackStatus.onNext(
                            Stopped(given)
                        )
                        else -> audioPlayBackStatus.onNext(PlayBackState.Paused(given))
                    }
                }
            }
        })

        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioPlayBackStatus
            .subscribe {
                when (it) {
                    is Playing -> {
                        audioManager.isSpeakerphoneOn = false
                    }
                    else -> audioManager.mode = AudioManager.MODE_NORMAL
                }
            }.disposedBy(disposeBag)

        audioControl.subscribe { playBackAction ->
            when (playBackAction) {
                is PlayBackAction.Play -> {
                    playerNotificationManager.setPlayer(player)
                    // No need to seek here; that'll be done in 'changeAudio' if needed
                    changeAudio(playBackAction.audioFile, playBackAction.audioModel)
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
                        audioPlayBackStatus.onNext(Stopped(audioFile))
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
            this,
            FOREGROUND_CHANNEL_ID,
            edu.artic.media.R.string.tour_audio_channel_name,
            edu.artic.media.R.string.tour_audio_channel_name,
            NotificationUtil.IMPORTANCE_LOW
        )
        playerNotificationManager = PlayerNotificationManager.Builder(
            this, NOTIFICATION_ID,
            FOREGROUND_CHANNEL_ID
        ).setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                //TODO make it dynamic so that activity that started the audio stream will be the destination of Intent
                val notificationIntent = "edu.artic.audio".asDeepLinkIntent()
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getActivity(
                        this@AudioPlayerService,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_MUTABLE
                    )
                } else {
                    PendingIntent.getActivity(
                        this@AudioPlayerService,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return null
            }

            override fun getCurrentContentTitle(player: Player): CharSequence {
                return playable?.getPlayableTitle().orEmpty()
            }


            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback,
            ): Bitmap? {
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
        }).setNotificationListener(object : NotificationListener {
            override fun onNotificationPosted(
                notificationId: Int,
                notification: Notification,
                ongoing: Boolean,
            ) {
                super.onNotificationPosted(notificationId, notification, ongoing)
                if (ongoing) // allow notification to be dismissed if player is stopped
                    startForeground(notificationId, notification)
                else
                    stopForeground(STOP_FOREGROUND_DETACH)
            }

            override fun onNotificationCancelled(
                notificationId: Int,
                dismissedByUser: Boolean,
            ) {
                super.onNotificationCancelled(notificationId, dismissedByUser)
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        })
            .setChannelNameResourceId(edu.artic.media.R.string.tour_audio_channel_name)
            .setChannelDescriptionResourceId(edu.artic.media.R.string.exo_download_description)
            .build()

        playerNotificationManager.apply {
            setUseStopAction(true)
            setUseFastForwardAction(true)
            setUseRewindAction(true)
            setPlayer(player)
            setSmallIcon(edu.artic.media.R.drawable.icn_notification)
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

    fun changeAudio(_articObject: Playable, audio: AudioFileModel, resetPosition: Boolean = false) {

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
                    analyticsTracker.reportEvent(
                        EventCategoryName.PlayBack,
                        AnalyticsAction.playbackInterrupted,
                        articObject.getPlayableTitle().orEmpty()
                    )
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


    private fun buildMediaSource(uri: Uri): MediaSource {
        return ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(uri))
    }

    override fun onDestroy() {
        super.onDestroy()

        (currentTrack as BehaviorSubject).value?.let { audioFile ->
            audioPlayBackStatus.onNext(Stopped(audioFile))
        }

        playerNotificationManager.setPlayer(null)
        player.release()

        // Make sure to replace 'disposeBag' with a new instance in ::onCreate.
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
     * @see changeAudio
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
        analyticsTracker.reportEvent(
            EventCategoryName.PlayBack,
            AnalyticsAction.playbackInterrupted,
            (currentTrack as BehaviorSubject).value?.title.orEmpty()
        )
        audioControl.onNext(AudioPlayerService.PlayBackAction.Stop())
    }

    /**
     * Returns currently playing [ArticAudioFile].
     * [ArticObject] has multiple [ArticAudioFile]s, so this function returns the currently selected
     * one.
     *
     * If the [playable] is not [ArticObject], this method returns [null].
     */
    fun getActiveFileModel(): ArticAudioFile? {
        val currentTrack: AudioFileModel? = (currentTrack as BehaviorSubject).value
        return if (currentTrack != null) {
            playable?.let { currentlyPlayingObject ->
                when (currentlyPlayingObject) {
                    is ArticObject -> currentlyPlayingObject.audioCommentary
                        .find { it.audioFile?.nid == currentTrack.audioGroupId }
                        ?.audioFile
                    else -> null
                }
            }
        } else {
            return null
        }

    }
}

/**
 * Kotlin(version:1.2.51) was unable to resolve this class when it was defined anonymously,
 * so had to create this class.
 */
class BitmapCallbackTarget(
    private val service: AudioPlayerService,
    private val callback: PlayerNotificationManager.BitmapCallback,
) : SimpleTarget<Bitmap>() {
    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        callback.onBitmap(resource)
        service.currentBitmap = resource
    }
}