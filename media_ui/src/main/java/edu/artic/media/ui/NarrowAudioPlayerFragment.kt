package edu.artic.media.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.FragmentManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.media.audio.AudioPlayerService
import edu.artic.navigation.NavigationConstants
import edu.artic.ui.BaseFragment
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_bottom_audio_player.*
import kotlin.reflect.KClass


/**
 * Houses the bottom audio player functionality.
 *
 * Adding this Fragment to an activity directly will provide all of our standard audio UI and
 * playback features.
 *
 * NB: This binds and unbinds with the background [AudioPlayerService] in [onViewCreated] and
 * [onDestroyView] (respectively speaking; see the linked audio player documentation for
 * details). For that reason, it must be bound to an [Activity][android.app.Activity].
 *
 * @author Sameer Dhakal (Fuzz)
 */
class NarrowAudioPlayerFragment : BaseViewModelFragment<NarrowAudioPlayerViewModel>() {

    override val viewModelClass: KClass<NarrowAudioPlayerViewModel>
        get() = NarrowAudioPlayerViewModel::class

    override val title: String
        get() = ""

    override val layoutResId: Int
        get() = R.layout.fragment_bottom_audio_player

    override val screenCategory: ScreenCategoryName?
        get() = null

    override val overrideStatusBarColor: Boolean
        get() = false

    var boundService: AudioPlayerService? = null
    var audioIntent: Intent? = null

    /**
     * Upon successful service connection sends the service instance to observers.
     */
    val observableAudioService: BehaviorSubject<AudioPlayerService> = BehaviorSubject.create()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerServiceBinder
            boundService = binder.getService()
            setUpAudioServiceBindings()

            /**
             * Emits instance of AudioPlayerService only when the connection is successful.
             */
            boundService?.let {
                observableAudioService.onNext(it)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioIntent = AudioPlayerService.getLaunchIntent(requireContext())
        requireActivity().startService(audioIntent)
        requireActivity().bindService(audioIntent, serviceConnection, 0)
        requireView().visibility = View.GONE
        closePlayer.setOnClickListener {
            boundService?.stopPlayer()
            requireView().visibility = View.GONE
        }

        exo_play.setOnClickListener {
            boundService?.resumePlayer()
        }

        exo_pause.setOnClickListener {
            boundService?.pausePlayer()
        }

        trackTitle.setOnClickListener {
            val intent = NavigationConstants.AUDIO.asDeepLinkIntent()
            intent.putExtras(argsBundle(true))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(serviceConnection)
    }

    /**
     * Binds the current track info and audio control actions with the service.
     */
    private fun setUpAudioServiceBindings() {
        
        boundService?.let { audioService ->
            audioService.currentTrack
                    .subscribe { audioFile ->
                        trackTitle.text = audioFile.title
                    }.disposedBy(disposeBag)

            audioService.audioPlayBackStatus
                    .map { it is AudioPlayerService.PlayBackState.Playing }
                    .bindToMain(exo_pause.visibility())
                    .disposedBy(disposeBag)

            audioService.audioPlayBackStatus
                    .map { it is AudioPlayerService.PlayBackState.Paused }
                    .bindToMain(exo_play.visibility())
                    .disposedBy(disposeBag)

            audioService.audioPlayBackStatus
                    .map { it is AudioPlayerService.PlayBackState.Paused || it is AudioPlayerService.PlayBackState.Playing }
                    .bindToMain(requireView().visibility())
                    .disposedBy(disposeBag)
        }
    }

    companion object {
        const val ARG_SKIP_TO_DETAILS = "ARG_SKIP_TO_DETAILS"

        /**
         * Use this to modify an [Intent] for launching the AudioActivity's
         * AudioDetailsFragment (in 'audio' module, of course).
         */
        fun argsBundle(shouldSkip: Boolean) = Bundle().apply {
            putBoolean(ARG_SKIP_TO_DETAILS, shouldSkip)
        }

    }

}

/**
 * Returns an Observable over the [AudioPlayerService]. Most callers will be fine
 * with the default arguments. A [NarrowAudioPlayerFragment] _**MUST**_ be in
 * the same activity as the calling fragment.
 *
 * NB: you might need to set [fm] if you're embedding the
 * [NarrowAudioPlayerFragment] in another fragment's View. Pass the relevant
 * [child fragment manager][BaseFragment.getChildFragmentManager] in that case.
 *
 * @param fragmentId id of a ViewGroup where [NarrowAudioPlayerFragment] is added
 * @param fm         which [FragmentManager] this id is registered with
 */
fun BaseFragment.getAudioServiceObservable(
        fragmentId: Int = R.id.newPlayer,
        fm: FragmentManager = requireActivity().supportFragmentManager
): Subject<AudioPlayerService> {
    val audioFragment = fm.findFragmentById(fragmentId) as NarrowAudioPlayerFragment
    return audioFragment.observableAudioService
}