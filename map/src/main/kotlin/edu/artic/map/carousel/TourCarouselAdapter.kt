package edu.artic.map.carousel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.image.GlideApp
import edu.artic.map.R
import edu.artic.map.databinding.TourCarouselCellBinding
import edu.artic.map.databinding.TourCarouselIntroCellBinding
import edu.artic.media.audio.AudioPlayerService
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Designed for a horizontally-scrolling list of
 * [tour stops][edu.artic.db.models.ArticTour.TourStop].
 *
 * The carousel counterpart to `edu.artic.tours.TourDetailsStopAdapter` in
 * the `:details` module.
 *
 * @author Sameer Dhakal (Fuzz)
 */
class TourCarouselAdapter :
    AutoHolderRecyclerViewAdapter<ViewBinding, TourCarousalBaseViewModel>() {

    override fun View.onBindView(
        item: TourCarousalBaseViewModel,
        holder: BaseViewHolder,
        position: Int,
    ) {
        when (item) {

            is TourCarousalIntroViewModel -> {
                val binding = holder.binding as TourCarouselIntroCellBinding
                binding.playTourIntroduction.setOnClickListener {
                    item.playTourIntro()
                }
            }

            is TourCarousalStopCellViewModel -> {
                with(holder.binding as TourCarouselCellBinding) {
                    item.titleText
                        .bindToMain(stopTitle.text())
                        .disposedBy(item.viewDisposeBag)
                    item.galleryText
                        .bindToMain(stopSubTitle.text())
                        .disposedBy(item.viewDisposeBag)

                    item.imageUrl
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            GlideApp.with(image.context)
                                .load(it)
                                .placeholder(R.drawable.placeholder_thumb)
                                .into(image)
                        }
                        .disposedBy(item.viewDisposeBag)

                    tourNumber.text = context.getString(R.string.tour_stop_position, position)

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

    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (viewType) {
            R.layout.tour_carousel_intro_cell -> TourCarouselIntroCellBinding.inflate(
                inflater,
                parent,
                false
            )
            else -> TourCarouselCellBinding.inflate(
                inflater,
                parent,
                false
            )
        }
        return BaseViewHolder(binding, viewType).apply {
            itemView.onHolderCreated(parent, viewType)
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