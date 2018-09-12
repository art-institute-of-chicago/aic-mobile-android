package edu.artic.map

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.media.audio.AudioPlayerService
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_amenity_search_cell.view.*
import kotlinx.android.synthetic.main.layout_artwork_search_cell.view.*

/**
@author Sameer Dhakal (Fuzz)
 */
class SearchedObjectsAdapter : AutoHolderRecyclerViewAdapter<SearchObjectBaseViewModel>() {

    override fun View.onBindView(item: SearchObjectBaseViewModel, position: Int) {
        when (item) {
            is DiningAnnotationViewModel -> {

                item.imageUrl
                        .map { it.isNotEmpty() }
                        .bindToMain(amenityImage.visibility())
                        .disposedBy(item.viewDisposeBag)

                item.imageUrl
                        .filter { it.isNotEmpty() }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            Glide.with(this)
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

            }
            is AnnotationViewModel -> {
                item.title
                        .bindToMain(amenityType.text())
                        .disposedBy(item.viewDisposeBag)

                item.description
                        .bindToMain(amenityDetails.text())
                        .disposedBy(item.viewDisposeBag)
            }
            is ArtworkViewModel -> {

                item.imageUrl
                        .filter { it.isNotEmpty() }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            Glide.with(this)
                                    .load(it)
                                    .into(objectImage)
                        }
                        .disposedBy(item.viewDisposeBag)

                item.artworkTitle
                        .bindToMain(objectTitle.text())
                        .disposedBy(item.viewDisposeBag)

                item.artistName
                        .bindToMain(artist.text())
                        .disposedBy(item.viewDisposeBag)

                item.objectType
                        .bindToMain(objectType.text())
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

                item.playState.subscribe {playBackState->
                    when (playBackState) {
                        is AudioPlayerService.PlayBackState.Playing -> {
                            playCurrent.visibility = View.INVISIBLE
                            pauseCurrent.visibility = View.VISIBLE
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

            }
        }
    }

    override fun getLayoutResId(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is DiningAnnotationViewModel -> R.layout.layout_amenity_search_cell
            is AnnotationViewModel -> R.layout.layout_amenity_search_cell
            is ArtworkViewModel -> R.layout.layout_artwork_search_cell
            else -> {
                0
                /* should never reach here*/
            }
        }
    }

    override fun onItemViewDetachedFromWindow(holder: BaseViewHolder, position: Int) {
        super.onItemViewDetachedFromWindow(holder, position)
        getItem(position).apply {
            cleanup()
            onCleared()
        }
    }
}