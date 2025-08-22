package edu.artic.tours

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.content.listing.R
import edu.artic.content.listing.databinding.CellAllToursIntroBinding
import edu.artic.content.listing.databinding.CellAllToursLayoutBinding
import edu.artic.image.GlideApp
import io.reactivex.subjects.Subject

/**
 * @author Sameer Dhakal (Fuzz)
 */

class AllToursAdapter(
    recyclerView: RecyclerView,
    introSubject: Subject<String>,
    viewDisposeBag: DisposeBag,
) : AutoHolderRecyclerViewAdapter<CellAllToursLayoutBinding, AllToursCellViewModel>() {

    private val introHolder = BaseViewHolder(
        CellAllToursIntroBinding.inflate(
            LayoutInflater.from(recyclerView.context),
            recyclerView,
            false
        ), R.layout.cell_all_tours_intro
    )

    init {
        addHeaderHolder(introHolder)
        introHolder.itemView.apply {
            introSubject
                .bindToMain((introHolder.binding as CellAllToursIntroBinding).intro.text())
                .disposedBy(viewDisposeBag)
        }
    }

    override fun View.onBindView(
        item: AllToursCellViewModel,
        holder: BaseViewHolder,
        position: Int,
    ) {
        val binding = holder.binding as CellAllToursLayoutBinding
        item.tourImageUrl.subscribe {
            GlideApp.with(context)
                .load(it)
                .error(R.drawable.placeholder_small)
                .placeholder(R.color.placeholderBackground)
                .into(binding.image)
        }.disposedBy(item.viewDisposeBag)

        item.tourTitle.bindToMain(binding.title.text()).disposedBy(item.viewDisposeBag)

        item.tourDescription
            .map {
                it.replace("&nbsp;", " ")
            }.subscribe { binding.description.text = it }
            .disposedBy(item.viewDisposeBag)

        item.tourStops
            .subscribe { binding.stops.text = context.getString(R.string.tour_stop_count, it) }
            .disposedBy(item.viewDisposeBag)

        item.tourDuration
            .bindToMain(binding.time.text())
            .disposedBy(item.viewDisposeBag)
    }

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItemOrNull(position)?.apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int): Int {
        return R.layout.cell_all_tours_layout
    }

}