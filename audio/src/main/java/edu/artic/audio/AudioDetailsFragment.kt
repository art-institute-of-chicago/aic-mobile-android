package edu.artic.audio


import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.isMyServiceRunning
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_audio_details.*
import kotlin.reflect.KClass

/**
 * @author Sameer Dhakal (Fuzz)
 */
class AudioDetailsFragment : BaseViewModelFragment<AudioDetailsViewModel>() {

    override val viewModelClass: KClass<AudioDetailsViewModel>
        get() = AudioDetailsViewModel::class

    override val title: String
        get() = ""

    override val layoutResId: Int
        get() = R.layout.fragment_audio_details

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.AudioPlayer

    override fun hasTransparentStatusBar(): Boolean = true

    var boundService: AudioPlayerService? = null
    var audioIntent: Intent? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerServiceBinder
            boundService = binder.getService()
            boundService?.let {
                audioPlayer.player = it.player
                it.player.refreshPlayBackState()
            }
            boundService?.setAudioUrl("http://cms.stream.publicradio.org/cms.mp3")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioIntent = Intent(context, AudioPlayerService::class.java)
        if (activity?.isMyServiceRunning(AudioPlayerService::class.java) != true) {
            audioIntent?.action = MusicConstants.ACTION.PLAY_ACTION
            activity?.startService(audioIntent)
        }
        activity?.bindService(audioIntent, serviceConnection, 0)
    }
}
