package edu.artic.audioui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.media.audio.AudioPlayerService
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
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
class BottomAudioPlayerFragment : BaseViewModelFragment<BottomAudioPlayerViewModel>() {

    override val viewModelClass: KClass<BottomAudioPlayerViewModel>
        get() = BottomAudioPlayerViewModel::class

    override val title: String
        get() = ""

    override val layoutResId: Int
        get() = R.layout.fragment_bottom_audio_player

    override val screenCategory: ScreenCategoryName?
        get() = null

    var boundService: AudioPlayerService? = null
    var audioIntent: Intent? = null


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerServiceBinder
            boundService = binder.getService()
            setUpAudioServiceBindings()
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
            startActivity(NavigationConstants.AUDIO.asDeepLinkIntent())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(serviceConnection)
    }


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

}