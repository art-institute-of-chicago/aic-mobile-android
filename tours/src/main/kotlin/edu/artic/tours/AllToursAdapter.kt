package edu.artic.tours

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import kotlinx.android.synthetic.main.cell_all_tours_layout.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */

class AllToursAdapter : AutoHolderRecyclerViewAdapter<AllToursCellViewModel>() {


    override fun View.onBindView(item: AllToursCellViewModel, position: Int) {

        item.tourImageUrl.subscribe {
            Glide.with(context)
                    .load(it)
                    .into(image)
        }.disposedBy(item.viewDisposeBag)

        item.tourTitle.bindToMain(title.text()).disposedBy(item.viewDisposeBag)

        item.tourDescription
                .map {
                    it.replace("&nbsp;", " ")
                }.subscribe { description.text = it }
                .disposedBy(item.viewDisposeBag)

        item.tourStops
                .map {
                    "$it ${context.getString(R.string.stops)}"
                }.subscribe { stops.text = it.toString() }
                .disposedBy(item.viewDisposeBag)

        item.tourDuration
                .bindToMain(time.text())
                .disposedBy(item.viewDisposeBag)
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.cell_all_tours_layout
    }

}