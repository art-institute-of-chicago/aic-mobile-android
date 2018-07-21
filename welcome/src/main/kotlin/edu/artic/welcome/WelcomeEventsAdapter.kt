package edu.artic.welcome

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import kotlinx.android.synthetic.main.welcome_event_cell_layout.view.*


/**
 * @author Sameer Dhakal (Fuzz)
 */

class WelcomeEventsAdapter : AutoHolderRecyclerViewAdapter<WelcomeEventCellViewModel>() {
    override fun View.onBindView(item: WelcomeEventCellViewModel, position: Int) {
        item.eventTitle
                .bindToMain(title.text())
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
                    Glide.with(context)
                            .load(it)
                            .into(image)
                }.disposedBy(item.viewDisposeBag)

    }

    override fun getLayoutResId(position: Int): Int = R.layout.welcome_event_cell_layout

}
