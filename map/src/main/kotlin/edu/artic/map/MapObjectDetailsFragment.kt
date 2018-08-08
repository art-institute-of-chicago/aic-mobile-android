package edu.artic.map

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.bumptech.glide.Glide
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tourStopTitle.text = mapObject?.title
        tourStopGallery.text = mapObject?.galleryLocation
        Glide.with(this)
                .load(mapObject?.largeImageFullPath)
                .into(image)
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