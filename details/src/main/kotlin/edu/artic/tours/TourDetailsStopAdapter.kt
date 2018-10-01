package edu.artic.tours

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.details.R
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.cell_tour_details_stop.view.*

/**
 * Adapter for [tour stops][edu.artic.db.models.ArticTour.TourStop]. Does not include
 * the so-called 'tour intro'; that is handled by the [TourDetailsFragment].
 *
 * For the carousel screen, see `edu.artic.map.carousel.TourCarouselAdapter` in
 * the `:map` module.
 */
class TourDetailsStopAdapter : AutoHolderRecyclerViewAdapter<TourDetailsStopCellViewModel>() {

    override fun View.onBindView(item: TourDetailsStopCellViewModel, position: Int) {
        item.titleText
                .bindToMain(tourStopTitle.text())
                .disposedBy(item.viewDisposeBag)
        item.galleryText
                .bindToMain(tourStopGallery.text())
                .disposedBy(item.viewDisposeBag)

        // The TourIntro (which would be labeled 0) is bound by the fragment separately.
        // TODO: Integrate that transform of the `ArticTour` as a separate ViewModel.
        tourNumber.text = (position + 1).toString()

        item.imageUrl
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Glide.with(this)
                            .load(it)
                            .into(image)
                }
                .disposedBy(item.viewDisposeBag)
    }

    override fun getLayoutResId(position: Int) = R.layout.cell_tour_details_stop

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
            onCleared()
        }
    }
}