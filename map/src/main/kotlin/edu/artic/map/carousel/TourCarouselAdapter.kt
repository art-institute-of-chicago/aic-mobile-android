package edu.artic.map.carousel

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.map.R
import edu.artic.media.audio.AudioPlayerService
import com.jakewharton.rxbinding2.widget.text
import kotlinx.android.synthetic.main.tour_carousel_cell.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */
class TourCarouselAdapter : AutoHolderRecyclerViewAdapter<TourCarousalBaseViewModel>() {

    override fun View.onBindView(item: TourCarousalBaseViewModel, position: Int) {
        when (item) {
            is TourCarousalStopCellViewModel -> {
                item.titleText
                        .bindToMain(stopTitle.text())
                        .disposedBy(item.viewDisposeBag)
                item.galleryText
                        .bindToMain(stopSubTitle.text())
                        .disposedBy(item.viewDisposeBag)

                item.imageUrl
                        .subscribe {
                            Glide.with(this)
                                    .load(it)
                                    .into(image)
                        }
                        .disposedBy(item.viewDisposeBag)

                item.stopNumber
                        .bindToMain(tourNumber.text())
                        .disposedBy(item.viewDisposeBag)

                item.viewPlayBackState
                        .subscribe { playBackState ->
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

                        }
                        .disposedBy(item.viewDisposeBag)

                playCurrent.setOnClickListener {
                    item.playCurrentObject()
                }

                pauseCurrent.setOnClickListener {
                    item.pauseCurrentObject()
                }
            }
        }

    }


    override fun getLayoutResId(position: Int): Int {
        /**
         * Position 0 is used for the tour introduction.
         */
        return if (position == 0) {
            R.layout.tour_carousel_intro_cell
        } else {
            R.layout.tour_carousel_cell
        }
    }

}