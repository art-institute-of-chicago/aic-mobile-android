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
    override fun View.onBindView(event: WelcomeEventCellViewModel, position: Int) {
        event.eventTitle
                .bindToMain(title.text())
                .disposedBy(event.viewDisposeBag)

        event.eventShortDescription
                .bindToMain(description.text())
                .disposedBy(event.viewDisposeBag)

        event.eventTime
                .bindToMain(date.text())
                .disposedBy(event.viewDisposeBag)

        event.eventImageUrl
                .filter { it.isNotEmpty() }
                .subscribe {
                    Glide.with(context)
                            .load(it)
                            .into(image)
                }.disposedBy(event.viewDisposeBag)

    }

    override fun getLayoutResId(position: Int): Int = R.layout.welcome_event_cell_layout

}
