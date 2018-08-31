package artic.edu.search

import android.support.v4.content.ContextCompat
import android.view.View
import artic.edu.search.DefaultSearchSuggestionsFragment.Companion.MAX_ARTWORKS_PER_ROW
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textRes
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_cell_amenity.view.*
import kotlinx.android.synthetic.main.layout_cell_header.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_keyword.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_map_object.view.*

/**
 * Adapter for loading default search suggestions
 * @author Sameer Dhakal (Fuzz)
 */
class DefaultSuggestionAdapter : AutoHolderRecyclerViewAdapter<SearchBaseCellViewModel>() {
    override fun View.onBindView(item: SearchBaseCellViewModel, position: Int) {
        when (item) {
            is TextCellViewModel -> {
                item.text
                        .bindTo(suggestedKeyword.text())
                        .disposedBy(item.viewDisposeBag)
            }
            is HeaderCellViewModel -> {
                item.text
                        .bindTo(headerText.textRes())
                        .disposedBy(item.viewDisposeBag)
            }
            is CircularCellViewModel -> {
                item.imageUrl
                        .subscribe {
                            Glide.with(this)
                                    .load(it)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(circularImage)
                        }
                        .disposedBy(item.viewDisposeBag)
            }
            is AmenitiesCellViewModel -> {
                icon.setImageDrawable(ContextCompat.getDrawable(icon.context, item.value))
            }
        }
    }

    override fun getLayoutResId(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is HeaderCellViewModel -> return R.layout.layout_cell_header
            is TextCellViewModel -> return R.layout.layout_cell_suggested_keyword
            is AmenitiesCellViewModel -> return R.layout.layout_cell_amenity
            is DividerViewModel -> return R.layout.layout_cell_divider
            is CircularCellViewModel -> R.layout.layout_cell_suggested_map_object
            else -> R.layout.layout_cell_suggested_map_object
        }
    }

    override fun onItemViewDetachedFromWindow(holder: BaseViewHolder, position: Int) {
        super.onItemViewDetachedFromWindow(holder, position)
        getItem(position).apply {
            cleanup()
            onCleared()
        }
    }

    /**
     * Span size for the artworks should be 5
     */
    fun getSpanCount(position: Int): Int {
        val cell = getItem(position)
        return if (cell is CircularCellViewModel || cell is AmenitiesCellViewModel) {
            1
        } else {
            MAX_ARTWORKS_PER_ROW
        }
    }
}

