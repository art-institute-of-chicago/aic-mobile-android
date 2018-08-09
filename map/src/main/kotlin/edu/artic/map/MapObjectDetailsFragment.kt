package edu.artic.map

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import dagger.android.support.AndroidSupportInjection
import edu.artic.db.models.ArticObject
import kotlinx.android.synthetic.main.fragment_map_object_details.*

/**
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsFragment : Fragment() {

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
        val currentArticObject = boundService?.getCurrentObject()

        if (currentArticObject?.nid == mapObject?.nid) {
            displayPlayButton()
        } else {
            displayPause()
        }
        
        mapObject?.let { mapObj ->

            playCurrent.setOnClickListener {
                boundService?.resumeCurrentAudio(mapObj)
            }

            pauseCurrent.setOnClickListener {
                boundService?.pauseCurrentAudio()
            }

        }

    }

    private fun displayPause() {
        playCurrent.visibility = View.VISIBLE
        pauseCurrent.visibility = View.INVISIBLE
    }

    private fun displayPlayButton() {
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
}