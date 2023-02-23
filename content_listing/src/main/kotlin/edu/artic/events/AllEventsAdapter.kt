package edu.artic.events

import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.content.listing.R
import edu.artic.content.listing.databinding.CellAllEventsLayoutBinding
import edu.artic.image.GlideApp


/**
 * @author Sameer Dhakal (Fuzz)
 */

class AllEventsAdapter : AutoHolderRecyclerViewAdapter<AllEventsCellBaseViewModel>() {

    override fun View.onBindView(
        item: AllEventsCellBaseViewModel,
        holder: BaseViewHolder,
        position: Int,
    ) {
        val binding = holder.binding as CellAllEventsLayoutBinding
        if (item is AllEventsCellViewModel) {
            binding.image.visibility = View.VISIBLE
            binding.title.visibility = View.VISIBLE
            binding.description.visibility = View.VISIBLE
            binding.dateTime.visibility = View.VISIBLE
            binding.spacerLine.visibility = View.VISIBLE
            binding.headerText.visibility = View.GONE

            item.eventImageUrl.subscribe {
                GlideApp.with(context)
                    .load(it)
                    .placeholder(R.color.placeholderBackground)
                    .error(R.drawable.placeholder_small)
                    .into(binding.image)
            }.disposedBy(item.viewDisposeBag)

            item.eventTitle
                .bindToMain(binding.title.text())
                .disposedBy(item.viewDisposeBag)

            item.eventDescription
                .bindToMain(binding.description.text())
                .disposedBy(item.viewDisposeBag)

            item.eventDateTime
                .bindToMain(binding.dateTime.text())
                .disposedBy(item.viewDisposeBag)
        } else if (item is AllEventsCellHeaderViewModel) {
            binding.image.visibility = View.GONE
            binding.title.visibility = View.GONE
            binding.description.visibility = View.GONE
            binding.dateTime.visibility = View.GONE
            binding.spacerLine.visibility = View.GONE
            binding.headerText.visibility = View.VISIBLE

            item.text
                .bindToMain(binding.headerText.text())
                .disposedBy(item.viewDisposeBag)
        }


    }

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.cell_all_events_layout
    }

}