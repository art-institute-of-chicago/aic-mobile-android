package edu.artic.welcome

import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.image.GlideApp
import kotlinx.android.synthetic.main.welcome_on_view_cell_layout.view.*

/**
 * @author Sameer Dhakal (Fuzz)
 */

class OnViewAdapter : AutoHolderRecyclerViewAdapter<WelcomeExhibitionCellViewModel>() {

    override fun View.onBindView(item: WelcomeExhibitionCellViewModel, position: Int) {

        item.exhibitionTitleStream
                .bindToMain(exhibitionTitle.text())
                .disposedBy(item.viewDisposeBag)

        item.exhibitionDate
                .map {
                    context.getString(R.string.throughDate, it)
                }
                .bindToMain(exhibitionDate.text())
                .disposedBy(item.viewDisposeBag)

        item.exhibitionImageUrl
                .filter { it.isNotEmpty() }
                .subscribe {
                    GlideApp.with(context)
                            .load(it)
                            .placeholder(R.drawable.square_placeholder)
                            .into(image)
                }.disposedBy(item.viewDisposeBag)

        this.image.transitionName = item.exhibition.title
    }

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.welcome_on_view_cell_layout
    }

}

