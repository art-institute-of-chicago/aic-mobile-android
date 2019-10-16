package edu.artic.welcome

import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.events.EventCellViewModel
import edu.artic.image.GlideApp
import kotlinx.android.synthetic.main.welcome_event_cell_layout.view.*


/**
 * @author Sameer Dhakal (Fuzz)
 */

class WelcomeEventsAdapter : AutoHolderRecyclerViewAdapter<EventCellViewModel>() {
    override fun View.onBindView(item: EventCellViewModel, position: Int) {

        item.eventTitle
                .bindToMain(title.text())
                .disposedBy(item.viewDisposeBag)

        item.eventTitle
                .subscribe { image.transitionName = it }
                .disposedBy(item.viewDisposeBag)

        item.eventDescription
                .bindToMain(description.text())
                .disposedBy(item.viewDisposeBag)

        item.eventDateTime
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

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int): Int = R.layout.welcome_event_cell_layout

}
