package edu.artic.welcome

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import kotlinx.android.synthetic.main.tour_card_layout.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */

class ToursAdapter : AutoHolderRecyclerViewAdapter<TourCellViewModel>() {


    override fun View.onBindView(item: TourCellViewModel, position: Int) {

        item.tourImageUrl.subscribe {
            Glide.with(context)
                    .load(it)
                    .into(image)
        }.disposedBy(item.viewDisposeBag)

        item.tourTitle.bindToMain(tourTitle.text()).disposedBy(item.viewDisposeBag)

        item.tourDescription
                .map {
                    it.replace("&nbsp;", " ")
                }.subscribe { tourDescription.text = it }
                .disposedBy(item.viewDisposeBag)

        item.tourStops
                .map {
                    "$it ${context.getString(R.string.stops)}"
                }.subscribe { stops.text = it.toString() }
                .disposedBy(item.viewDisposeBag)
        
        item.tourDuration.bindToMain(tourTime.text()).disposedBy(item.viewDisposeBag)
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.tour_card_layout
    }

}