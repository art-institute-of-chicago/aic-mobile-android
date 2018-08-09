package edu.artic.audio


import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.listenerAnimateSharedTransaction
import edu.artic.base.utils.updateDetailTitle
import edu.artic.db.models.ArticObject
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.refreshPlayBackState
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
            updateView(boundService?.articObject)
        }
    }

    fun updateView(articObject: ArticObject?) {
        /**
         * Get the first audio commentary file
         */
        val audioFile = articObject?.audioCommentary?.first()?.audioFile

        expandedTitle.text = audioFile?.title
        toolbarTitle.text = audioFile?.title

        if (!articObject?.artistCulturePlaceDelim.isNullOrBlank()) {
            artistCulturePlaceDenim.visibility = View.VISIBLE
            artistCulturePlaceDenim.text = articObject?.artistCulturePlaceDelim?.replace("\r","\n")
        } else {
            artistCulturePlaceDenim.visibility = View.GONE
        }

        if (!audioFile?.transcript.isNullOrBlank()) {
            transcript.visibility = View.VISIBLE
            transcript.setContentText(audioFile?.transcript)
        } else {
            transcript.visibility = View.GONE
        }

        if (!audioFile?.credits.isNullOrBlank()) {
            credit.visibility = View.VISIBLE
            credit.setContentText(audioFile?.credits)
        } else {
            credit.visibility = View.GONE
        }

        val options = RequestOptions()
                .dontAnimate()
                .dontTransform()

        Glide.with(this)
                .load(articObject?.largeImageFullPath)
                .apply(options)
                .listenerAnimateSharedTransaction(this, audioImage)
                .into(audioImage)
    }

    override fun onResume() {
        super.onResume()
        audioIntent = Intent(context, AudioPlayerService::class.java)
        activity?.bindService(audioIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.unbindService(serviceConnection)
    }

}
