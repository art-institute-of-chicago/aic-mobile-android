package edu.artic.tours.carousel

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.media.audio.AudioPlayerService
import edu.artic.tours.R
import kotlinx.android.synthetic.main.tour_carousel_cell.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */
class TourCarouselAdapter : AutoHolderRecyclerViewAdapter<TourCarousalStopCellViewModel>() {

    override fun View.onBindView(item: TourCarousalStopCellViewModel, position: Int) {
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


    override fun getLayoutResId(position: Int): Int {
        return R.layout.tour_carousel_cell
    }

}