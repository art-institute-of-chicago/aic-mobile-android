package edu.artic.welcome

import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.image.GlideApp
import edu.artic.welcome.databinding.WelcomeEventCellLayoutBinding


/**
 * @author Sameer Dhakal (Fuzz)
 */

class WelcomeEventsAdapter :
    AutoHolderRecyclerViewAdapter<WelcomeEventCellLayoutBinding, WelcomeEventCellViewModel>() {
    override fun View.onBindView(
        item: WelcomeEventCellViewModel,
        holder: BaseViewHolder,
        position: Int,
    ) {
        with(holder.binding as WelcomeEventCellLayoutBinding) {
            item.eventTitle
                .bindToMain(title.text())
                .disposedBy(item.viewDisposeBag)


            item.eventTitle
                .subscribe { image.transitionName = it }
                .disposedBy(item.viewDisposeBag)

            item.eventShortDescription
                .bindToMain(description.text())
                .disposedBy(item.viewDisposeBag)

            item.eventTime
                .bindToMain(date.text())
                .disposedBy(item.viewDisposeBag)

            item.eventImageUrl
                .filter { it.isNotEmpty() }
                .subscribe {
                    GlideApp.with(context)
                        .load(it)
                        .placeholder(R.color.placeholderBackground)
                        .error(R.drawable.placeholder_medium_rect)
                        .into(image)
                }.disposedBy(item.viewDisposeBag)
        }

    }

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int): Int = R.layout.welcome_event_cell_layout

}
