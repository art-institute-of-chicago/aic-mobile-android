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
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.listenerAnimateSharedTransaction
import edu.artic.base.utils.updateDetailTitle
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
            viewModel.audioObject = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerServiceBinder
            boundService = binder.getService()
            viewModel.audioObject = boundService?.articObject

            boundService?.let {
                audioPlayer.player = it.player
                it.player.refreshPlayBackState()
            }

        }
    }

    override fun setupBindings(viewModel: AudioDetailsViewModel) {

        viewModel.title.subscribe {
            expandedTitle.text = it
            toolbarTitle.text = it
        }.disposedBy(disposeBag)

        val options = RequestOptions()
                .dontAnimate()
                .dontTransform()

        viewModel.image
                .map { it.isNotEmpty() }
                .bindToMain(audioImage.visibility())
                .disposedBy(disposeBag)

        viewModel.image.subscribe {
            Glide.with(this)
                    .load(it)
                    .apply(options)
                    .listenerAnimateSharedTransaction(this, audioImage)
                    .into(audioImage)
        }.disposedBy(disposeBag)

        viewModel.authorCulturalPlace
                .map { it.isNotEmpty() }
                .bindToMain(artistCulturePlaceDenim.visibility())
                .disposedBy(disposeBag)

        viewModel.authorCulturalPlace
                .bindToMain(artistCulturePlaceDenim.text())
                .disposedBy(disposeBag)

        viewModel.transcript
                .map { it.isNotEmpty() }
                .bindToMain(transcript.visibility())
                .disposedBy(disposeBag)

        viewModel.transcript
                .subscribe {
                    transcript.setContentText(it)
                }.disposedBy(disposeBag)

        viewModel.credits
                .map { it.isNotEmpty() }
                .bindToMain(credit.visibility())
                .disposedBy(disposeBag)


        viewModel.credits
                .subscribe {
                    credit.setContentText(it)
                }.disposedBy(disposeBag)
    }


    override fun onResume() {
        super.onResume()
        audioIntent = AudioPlayerService.getLaunchIntent(requireContext())
        requireActivity().bindService(audioIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unbindService(serviceConnection)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }
    }
}
