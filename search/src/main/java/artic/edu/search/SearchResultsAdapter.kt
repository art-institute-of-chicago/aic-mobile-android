package artic.edu.search

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import kotlinx.android.synthetic.main.layout_cell_amenity.view.*
import kotlinx.android.synthetic.main.layout_cell_header.view.*
import kotlinx.android.synthetic.main.layout_cell_result_header.view.*
import kotlinx.android.synthetic.main.layout_cell_search_list_item.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_keyword.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_map_object.view.*

class SearchResultsAdapter : AutoHolderRecyclerViewAdapter<SearchResultBaseCellViewModel>() {

    companion object {
        const val MAX_ARTWORKS_PER_ROW = 5
    }

    override fun View.onBindView(item: SearchResultBaseCellViewModel, position: Int) {
        when (item) {

            is SearchResultHeaderCellViewModel -> {
                item.text
                        .bindToMain(title.text())
                        .disposedBy(item.viewDisposeBag)
            }

            is SearchResultOnMapHeaderCellViewModel -> {
                item.text
                        .bindToMain(headerText.text())
                        .disposedBy(item.viewDisposeBag)
            }
            is SearchResultBaseListItemViewModel -> {
                item.imageUrl
                        .subscribe {
                            Glide.with(context)
                                    .load(it)
                                    .into(image)
                        }.disposedBy(item.viewDisposeBag)
                item.isHeadphonesVisible
                        .bindToMain(headphonesIcon.visibility())
                        .disposedBy(item.viewDisposeBag)

                item.itemTitle
                        .bindToMain(itemTitle.text())
                        .disposedBy(item.viewDisposeBag)
                item.itemSubTitle
                        .bindToMain(itemSubTitle.text())
                        .disposedBy(item.viewDisposeBag)

                when (item) {
                    is SearchResultArtworkCellViewModel -> {

                    }
                    is SearchResultExhibitionCellViewModel -> {

                    }
                    is SearchResultTourCellViewModel -> {
                    }
                }
            }
            is SearchResultTextCellViewModel -> {
                item.text
                        .bindTo(suggestedKeyword.text())
                        .disposedBy(item.viewDisposeBag)
            }
            is SearchResultCircularCellViewModel -> {
                item.imageUrl
                        .subscribe {
                            Glide.with(this)
                                    .load(it)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(circularImage)
                        }
                        .disposedBy(item.viewDisposeBag)
            }
            is SearchResultAmenitiesCellViewModel -> {
                if (item.value != 0) {
                    icon.setImageResource(item.value)
                } else {
                    icon.setImageDrawable(null)
                }
            }
            else -> {

            }
        }
    }

    override fun getLayoutResId(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is SearchResultHeaderCellViewModel -> R.layout.layout_cell_result_header
            is SearchResultOnMapHeaderCellViewModel -> R.layout.layout_cell_header
            is SearchResultBaseListItemViewModel -> R.layout.layout_cell_search_list_item
            is SearchResultTextCellViewModel -> R.layout.layout_cell_suggested_keyword
            is SearchResultEmptyCellViewModel -> 0
            is SearchResultAmenitiesCellViewModel -> R.layout.layout_cell_amenity
            else -> R.layout.layout_cell_suggested_map_object
        }
    }


    fun getSpanCount(position: Int): Int {
        val cell = getItemOrNull(position)
        return if (cell is SearchResultCircularCellViewModel || cell is SearchResultAmenitiesCellViewModel) {
            1
        } else {
            SearchResultsAdapter.MAX_ARTWORKS_PER_ROW
        }
    }
}