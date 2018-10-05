package edu.artic.exhibitions

import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.image.GlideApp
import kotlinx.android.synthetic.main.cell_all_exhibitions_layout.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */

class AllExhibitionsAdapter : AutoHolderRecyclerViewAdapter<AllExhibitionsCellViewModel>() {

    override fun View.onBindView(item: AllExhibitionsCellViewModel, position: Int) {
        item.exhibitionImageUrl.subscribe {
            GlideApp.with(context)
                    .load(it)
                    .placeholder(R.drawable.placeholder_medium_square)
                    .into(image)
        }.disposedBy(item.viewDisposeBag)

        item.exhibitionTitle.bindToMain(title.text()).disposedBy(item.viewDisposeBag)
        item.exhibitionTitle.subscribe{image.transitionName = it}.disposedBy(item.viewDisposeBag)
        item.exhibitionEndDate
                .map {
                    context.getString(R.string.throughDate, it)
                }
                .bindToMain(description.text())
                .disposedBy(item.viewDisposeBag)
    }
    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.cell_all_exhibitions_layout
    }

}