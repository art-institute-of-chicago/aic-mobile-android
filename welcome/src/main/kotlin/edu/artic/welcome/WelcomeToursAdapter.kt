package edu.artic.welcome

import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.image.GlideApp
import kotlinx.android.synthetic.main.welcome_tour_summary_cell_layout.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */

class WelcomeToursAdapter : AutoHolderRecyclerViewAdapter<WelcomeTourCellViewModel>() {


    override fun View.onBindView(item: WelcomeTourCellViewModel, position: Int) {
        item.tourImageUrl.subscribe {
            GlideApp.with(context)
                    .load(it)
                    .placeholder(R.drawable.rect_default_image)
                    .into(image)
        }.disposedBy(item.viewDisposeBag)

        item.tourTitle.bindToMain(tourTitle.text()).disposedBy(item.viewDisposeBag)

        item.tourDescription
                .map {
                    it.replace("&nbsp;", " ")
                }.subscribe { tourDescription.text = it }
                .disposedBy(item.viewDisposeBag)

        item.tourStops
                .subscribe { stops.text = context.getString(R.string.stops, it) }
                .disposedBy(item.viewDisposeBag)

        item.tourDuration.bindToMain(tourTime.text()).disposedBy(item.viewDisposeBag)
    }

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.welcome_tour_summary_cell_layout
    }

}