package edu.artic.map

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterValue
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textRes
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.map.databinding.LayoutAmenitySearchCellBinding
import edu.artic.map.databinding.LayoutArtworkSearchCellBinding
import edu.artic.map.databinding.LayoutExhibitionSearchCellBinding
import edu.artic.media.audio.AudioPlayerService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

//import kotlinx.android.synthetic.main.layout_amenity_search_cell.view.*
//import kotlinx.android.synthetic.main.layout_artwork_search_cell.view.*
//import kotlinx.android.synthetic.main.layout_exhibition_search_cell.view.*

/**
@author Sameer Dhakal (Fuzz)
 */
class SearchedObjectsAdapter : AutoHolderRecyclerViewAdapter<SearchObjectBaseViewModel>() {

    override fun View.onBindView(
        item: SearchObjectBaseViewModel,
        holder: BaseViewHolder,
        position: Int,
    ) {
        when (item) {
            is DiningAnnotationViewModel -> {
                bindDiningAnnotationView(item, holder, this)
            }
            is AnnotationViewModel -> {
                with(holder.binding as LayoutAmenitySearchCellBinding) {
                    item.title
                        .bindToMain(amenityType.text())
                        .disposedBy(item.viewDisposeBag)

                    item.description
                        .bindToMain(amenityDetails.text())
                        .disposedBy(item.viewDisposeBag)
                }
            }
            is ExhibitionViewModel -> {
                bindExhibitionView(item, holder, this)
            }
            is ArtworkViewModel -> {
                bindArtworkView(item, holder, this)
            }
        }
    }

    fun bindDiningAnnotationView(
        item: DiningAnnotationViewModel,
        holder: BaseViewHolder,
        view: View,
    ) {
        with(holder.binding as LayoutAmenitySearchCellBinding) {
            item.imageUrl
                .map { it.isNotEmpty() }
                .bindToMain(amenityImage.visibility())
                .disposedBy(item.viewDisposeBag)

            item.imageUrl
                .filter { it.isNotEmpty() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Glide.with(view)
                        .load(it)
                        .into(amenityImage)
                }
                .disposedBy(item.viewDisposeBag)

            item.title
                .bindToMain(amenityType.text())
                .disposedBy(item.viewDisposeBag)

            item.description
                .bindToMain(amenityDetails.text())
                .disposedBy(item.viewDisposeBag)

            amenitySpace.visibility = View.GONE
        }
    }

    fun bindExhibitionView(item: ExhibitionViewModel, holder: BaseViewHolder, view: View) {
        with(holder.binding as LayoutExhibitionSearchCellBinding) {
            item.imageUrl
                .filter { it.isNotEmpty() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    exhibitionImage.post {
                        exhibitionImage.post {
                            Glide.with(view)
                                .load("$it?w=${exhibitionImage.measuredWidth}&h=${exhibitionImage.measuredWidth}")
                                .placeholder(R.color.placeholderBackground)
                                .error(R.drawable.placeholder_thumb)
                                .into(exhibitionImage)
                        }
                    }
                }
                .disposedBy(item.viewDisposeBag)
            item.title
                .bindToMain(exhibitionTitle.text())
                .disposedBy(item.viewDisposeBag)

            val galleryTitle = item.galleryTitle
                .subscribeOn(Schedulers.io())
                .share()

            galleryTitle
                .filterValue()
                .bindToMain(exhibitionGalleryTitle.text())
                .disposedBy(item.viewDisposeBag)

            galleryTitle
                .filter { it.value == null }
                .switchMap { item.floor }
                .bindToMain(exhibitionGalleryTitle.textRes())
                .disposedBy(item.viewDisposeBag)
        }
    }

    fun bindArtworkView(item: ArtworkViewModel, holder: BaseViewHolder, view: View) {
        with(holder.binding as LayoutArtworkSearchCellBinding)
        {
            item.imageUrl
                .filter { it.isNotEmpty() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Glide.with(view)
                        .load(it)
                        .placeholder(R.color.placeholderBackground)
                        .error(R.drawable.placeholder_thumb)
                        .into(objectImage)
                }
                .disposedBy(item.viewDisposeBag)

            item.artworkTitle
                .bindToMain(objectTitle.text())
                .disposedBy(item.viewDisposeBag)

            item.artistName
                .bindToMain(artist.text())
                .disposedBy(item.viewDisposeBag)

            item.artistName
                .map {
                    it.isNotEmpty()
                }
                .bindToMain(artist.visibility(View.GONE))
                .disposedBy(item.viewDisposeBag)

            item.gallery
                .map {
                    view.resources.getString(R.string.search_gallery_number, it)
                }
                .bindToMain(gallery.text())
                .disposedBy(item.viewDisposeBag)

            item.objectType
                .bindToMain(objectType.textRes())
                .disposedBy(item.viewDisposeBag)

            playCurrent.clicks()
                .defaultThrottle()
                .subscribe {
                    item.playAudioTranslation()
                }.disposedBy(item.viewDisposeBag)

            pauseCurrent.clicks()
                .defaultThrottle()
                .subscribe {
                    item.pauseAudioTranslation()
                }.disposedBy(item.viewDisposeBag)

            item.playState.subscribe { playBackState ->
                when (playBackState) {
                    is AudioPlayerService.PlayBackState.Playing -> {
                        view.post {
                            playCurrent.visibility = View.INVISIBLE
                            pauseCurrent.visibility = View.VISIBLE
                        }
                    }
                    is AudioPlayerService.PlayBackState.Paused -> {
                        playCurrent.visibility = View.VISIBLE
                        pauseCurrent.visibility = View.INVISIBLE
                    }
                    is AudioPlayerService.PlayBackState.Stopped -> {
                        playCurrent.visibility = View.VISIBLE
                        pauseCurrent.visibility = View.INVISIBLE
                    }
                }
            }.disposedBy(item.viewDisposeBag)

            item.hasAudio
                .bindToMain(playCurrent.visibility())
                .disposedBy(item.viewDisposeBag)
        }
    }

    override fun getLayoutResId(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is DiningAnnotationViewModel -> R.layout.layout_amenity_search_cell
            is AnnotationViewModel -> R.layout.layout_amenity_search_cell
            is ArtworkViewModel -> R.layout.layout_artwork_search_cell
            is ExhibitionViewModel -> R.layout.layout_exhibition_search_cell
            else -> {
                0
                /* should never reach here*/
            }
        }
    }

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItemOrNull(position)?.apply {
            cleanup()
        }
    }
}