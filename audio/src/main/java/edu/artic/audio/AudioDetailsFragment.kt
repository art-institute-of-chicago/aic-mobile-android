package edu.artic.audio


import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.updateDetailTitle
import edu.artic.db.models.ArticAudioFile
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
    private var audioIntent: Intent? = null

    private val audioFile: ArticAudioFile = getAudio()

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
            boundService?.setAudioObject(audioFile)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioIntent = Intent(context, AudioPlayerService::class.java)
        audioIntent?.action = MusicConstants.ACTION.PLAY_ACTION
        activity?.bindService(audioIntent, serviceConnection, BIND_AUTO_CREATE)

        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }

        expandedTitle.text = audioFile.title
        toolbarTitle.text = audioFile.title
    }

    fun getAudio(): ArticAudioFile {
        return ArticAudioFile("Justus Sustermans", null, "1", null, emptyList(), null, "http://aic-mobile-tours.artic.edu/sites/default/files/audio/882.mp3", null, null, null, null, "Justus Sustermans")
    }
}
