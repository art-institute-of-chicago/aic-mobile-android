package edu.artic.welcome

import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.image.GlideApp
import edu.artic.welcome.databinding.WelcomeOnViewCellLayoutBinding
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * @author Sameer Dhakal (Fuzz)
 */


class OnViewAdapter : AutoHolderRecyclerViewAdapter<WelcomeOnViewCellLayoutBinding,WelcomeExhibitionCellViewModel>() {

    override fun View.onBindView(
        item: WelcomeExhibitionCellViewModel,
        holder: BaseViewHolder,
        position: Int,
    ) {
        with(holder.binding as WelcomeOnViewCellLayoutBinding) {
            item.exhibitionTitleStream
                .bindToMain(exhibitionTitle.text())
                .disposedBy(item.viewDisposeBag)

            item.exhibitionDate
                .map {
                    context.getString(R.string.content_through_date, it)
                }
                .bindToMain(exhibitionDate.text())
                .disposedBy(item.viewDisposeBag)

            item.exhibitionImageUrl
                .filter { it.isNotEmpty() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    image.post {
                        GlideApp.with(context)
                            .load("$it?w=${image.measuredWidth}&h=${image.measuredHeight}")
                            .placeholder(R.color.placeholderBackground)
                            .error(R.drawable.placeholder_medium_square)
                            .into(image)
                    }
                }.disposedBy(item.viewDisposeBag)

            this.image.transitionName = item.exhibition.title
        }
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

