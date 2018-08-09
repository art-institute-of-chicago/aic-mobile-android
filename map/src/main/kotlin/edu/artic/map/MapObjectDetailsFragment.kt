package edu.artic.map

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy
import dagger.android.support.AndroidSupportInjection
import edu.artic.db.models.ArticAudioFile
import edu.artic.db.models.ArticObject
import edu.artic.media.audio.AudioPlayerService
import kotlinx.android.synthetic.main.fragment_map_object_details.*

/**
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsFragment : Fragment() {

    private val mapObject: ArticObject? by lazy { arguments?.getParcelable<ArticObject>(ARG_MAP_OBJECT) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_object_details, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    val disposeBag = DisposeBag()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tourStopTitle.text = mapObject?.title
        tourStopGallery.text = mapObject?.galleryLocation

        Glide.with(this)
                .load(mapObject?.largeImageFullPath)
                .into(image)

        val mapActivity = context as MapActivity
        val boundService = mapActivity.boundService
        val mapAudioObject: ArticAudioFile? = mapObject?.audioCommentary?.first()?.audioFile

        playCurrent.setOnClickListener {
            if (mapObject != null) {
                boundService?.audioControl?.onNext(AudioPlayerService.PlayBackAction.Play(mapObject as ArticObject))
            }
        }

        pauseCurrent.setOnClickListener {
            boundService?.audioControl?.onNext(AudioPlayerService.PlayBackAction.Pause())
        }
        /**
         * if the current track and selected map object's track are same
         */
        displayPlayButton()

        boundService?.audioPlayBackStatus
                ?.subscribe { playBackState ->

                    if (playBackState.articAudioFile == mapAudioObject) {

                        when (playBackState) {

                            is AudioPlayerService.PlayBackState.Playing -> {
                                displayPause()
                            }

                            is AudioPlayerService.PlayBackState.Paused -> {
                                displayPlayButton()
                            }

                            is AudioPlayerService.PlayBackState.Stopped -> {
                                displayPlayButton()
                            }
                        }
                    } else {
                        displayPlayButton()
                    }

                }
                ?.disposedBy(disposeBag)

    }

    private fun displayPause() {
        playCurrent.visibility = View.INVISIBLE
        pauseCurrent.visibility = View.VISIBLE

    }

    private fun displayPlayButton() {
        playCurrent.visibility = View.VISIBLE
        pauseCurrent.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposeBag.clear()
    }

    companion object {
        private val ARG_MAP_OBJECT = MapObjectDetailsFragment::class.java.simpleName

        fun create(articObject: ArticObject): MapObjectDetailsFragment {
            return MapObjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MAP_OBJECT, articObject)
                }
            }
        }
    }
}