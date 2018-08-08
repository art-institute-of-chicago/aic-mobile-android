package edu.artic.map

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy
import dagger.android.support.AndroidSupportInjection
import edu.artic.db.daos.ArticAudioFileDao
import edu.artic.db.models.ArticObject
import kotlinx.android.synthetic.main.fragment_map_object_details.*
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsFragment : Fragment() {

    @Inject
    lateinit var audioDao: ArticAudioFileDao
    val disposeBag = DisposeBag()
    private val mapObject by lazy { arguments?.getParcelable<ArticObject>(ARG_MAP_OBJECT) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_object_details, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tourStopTitle.text = mapObject?.title
        tourStopGallery.text = mapObject?.galleryLocation

        Glide.with(this)
                .load(mapObject?.largeImageFullPath)
                .into(image)

        val mapActivity = context as MapActivity
        val boundService = mapActivity.boundService

        mapObject?.audioCommentary?.first()?.audio?.let { audioId ->
            audioDao.getAudioById(audioId)
                    .subscribe { audioFile ->
                        Log.d("audio", audioFile.toString())
                        val currentAudioStream = boundService?.getCurrentAudio()
                        if (currentAudioStream?.nid == audioFile?.nid) {
                            dispalyPlayButton()
                        } else {
                            displayPause()
                        }
                        playCurrent.setOnClickListener {
                            boundService?.resumeCurrentAudio(audioFile)
                        }
                    }.disposedBy(disposeBag)
        }
        
        pauseCurrent.setOnClickListener {
            boundService?.pauseCurrentAudio()
        }
    }

    private fun displayPause() {
        playCurrent.visibility = View.VISIBLE
        pauseCurrent.visibility = View.INVISIBLE
    }

    private fun dispalyPlayButton() {
        playCurrent.visibility = View.INVISIBLE
        pauseCurrent.visibility = View.VISIBLE
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

    override fun onDestroy() {
        super.onDestroy()
    }
}