package edu.artic.map.carousel

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.map.R
import edu.artic.media.audio.AudioPlayerService
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.tour_carousel_cell.view.*
import kotlinx.android.synthetic.main.tour_carousel_intro_cell.view.*

/**
 * Designed for a horizontally-scrolling list of
 * [tour stops][edu.artic.db.models.ArticTour.TourStop].
 *
 * The carousel counterpart to `edu.artic.tours.TourDetailsStopAdapter` in
 * the `:details` module.
 *
 * @author Sameer Dhakal (Fuzz)
 */
class TourCarouselAdapter : AutoHolderRecyclerViewAdapter<TourCarousalBaseViewModel>() {

    override fun View.onBindView(item: TourCarousalBaseViewModel, position: Int) {
        when (item) {

            is TourCarousalIntroViewModel -> {
                playTourIntroduction.setOnClickListener {
                    item.playTourIntro()
                }
            }

            is TourCarousalStopCellViewModel -> {
                item.titleText
                        .bindToMain(stopTitle.text())
                        .disposedBy(item.viewDisposeBag)
                item.galleryText
                        .bindToMain(stopSubTitle.text())
                        .disposedBy(item.viewDisposeBag)

                item.imageUrl
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            Glide.with(this)
                                    .load(it)
                                    .into(image)
                        }
                        .disposedBy(item.viewDisposeBag)

                tourNumber.text = position.toString()

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