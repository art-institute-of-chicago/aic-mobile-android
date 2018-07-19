package edu.artic.welcome

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
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
                .subscribe {
                    val throughString = context.getString(R.string.through, it)
                    exhibitionDate.text = throughString
                }
                .disposedBy(item.viewDisposeBag)

        item.exhibitionImageUrl
                .filter { it.isNotEmpty() }
                .subscribe {
                    Glide.with(context)
                            .load(it)
                            .into(image)
                }.disposedBy(item.viewDisposeBag)
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.welcome_on_view_cell_layout
    }

}

