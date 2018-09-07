package artic.edu.search

import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import kotlinx.android.synthetic.main.layout_cell_result_header.view.*
import kotlinx.android.synthetic.main.layout_cell_search_list_item.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_keyword.view.*

class SearchResultsAdapter : AutoHolderRecyclerViewAdapter<SearchResultBaseCellViewModel>() {

    override fun View.onBindView(item: SearchResultBaseCellViewModel, position: Int) {
        when (item) {

            is SearchResultHeaderCellViewModel -> {
                item.text
                        .bindToMain(title.text())
                        .disposedBy(item.viewDisposeBag)
            }

            is SearchResultBaseListItemViewModel -> {
                item.imageUrl
                        .subscribe {
                            Glide.with(context)
                                    .load(it)
                                    .into(image)
                        }.disposedBy(item.viewDisposeBag)
                item.isHeadphonesVisisble
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
            else -> {

            }
        }
    }

    override fun getLayoutResId(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is SearchResultHeaderCellViewModel -> R.layout.layout_cell_result_header
            is SearchResultArtworkCellViewModel -> R.layout.layout_cell_search_list_item
            is SearchResultExhibitionCellViewModel -> R.layout.layout_cell_search_list_item
            is SearchResultTourCellViewModel -> R.layout.layout_cell_search_list_item
            is SearchResultTextCellViewModel -> R.layout.layout_cell_suggested_keyword
            is SearchResultEmptyCellViewModel -> 0
            else -> R.layout.layout_cell_suggested_map_object
        }
    }

}