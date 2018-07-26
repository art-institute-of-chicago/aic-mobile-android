package edu.artic.tours

import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.cell_all_tours_intro.view.*
import kotlinx.android.synthetic.main.cell_all_tours_layout.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */

class AllToursAdapter(recyclerView : RecyclerView, introSubject: Subject<String>, viewDisposeBag: DisposeBag) : AutoHolderRecyclerViewAdapter<AllToursCellViewModel>() {

    private val introHolder = BaseViewHolder(recyclerView, R.layout.cell_all_tours_intro)


    init {
        addHeaderHolder(introHolder)
        introHolder.itemView.apply {
            introSubject
                    .bindToMain(intro.text())
                    .disposedBy(viewDisposeBag)
        }
    }

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
                .subscribe { stops.text = context.getString(R.string.stops, it) }
                .disposedBy(item.viewDisposeBag)

        item.tourDuration
                .bindToMain(time.text())
                .disposedBy(item.viewDisposeBag)
    }

    override fun onItemViewDetachedFromWindow(holder: BaseViewHolder, position: Int) {
        super.onItemViewDetachedFromWindow(holder, position)
        getItemOrNull(position)?.apply {
            cleanup()
            onCleared()
        }
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.cell_all_tours_layout
    }

}