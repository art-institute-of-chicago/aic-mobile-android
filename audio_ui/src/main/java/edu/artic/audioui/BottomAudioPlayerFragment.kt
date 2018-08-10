package edu.artic.audioui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import edu.artic.media.audio.AudioPlayerService
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_bottom_audio_player.*


/**
 * Houses the bottom audio player functionality.
 * Adding the Fragment to activity directly will provide all the audio UI and playback feature.
 * @author Sameer Dhakal (Fuzz)
 */
class BottomAudioPlayerFragment : Fragment() {

    var boundService: AudioPlayerService? = null
    var audioIntent: Intent? = null

    var disposeBag = CompositeDisposable()
    lateinit var rootView: View


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


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_bottom_audio_player, container, false)
        return rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioIntent = Intent(requireContext(), AudioPlayerService::class.java)
        requireActivity().startService(audioIntent)
        requireActivity().bindService(audioIntent, serviceConnection, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(serviceConnection)
        disposeBag.clear()
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        rootView.visibility = View.GONE

        closePlayer.setOnClickListener {
            boundService?.stopPlayer()
            rootView.visibility = View.GONE
        }

        exo_play.setOnClickListener {
            boundService?.resumePlayer()
        }

        exo_pause.setOnClickListener {
            boundService?.pausePlayer()
        }

        trackTitle.setOnClickListener {
            startActivity(boundService?.getIntent())
        }
    }


    private fun setUpAudioServiceBindings() {
        boundService?.let { audioService ->

            audioService.currentTrack.subscribe { audioFile ->
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
                    .bindToMain(rootView.visibility())
                    .disposedBy(disposeBag)
        }
    }

}