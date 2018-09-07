package artic.edu.search

import android.view.View
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import kotlinx.android.synthetic.main.layout_cell_result_header.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_keyword.view.*

class SearchResultsAdapter : AutoHolderRecyclerViewAdapter<SearchResultBaseCellViewModel>() {

    override fun View.onBindView(item: SearchResultBaseCellViewModel, position: Int) {
        when (item) {
            is SearchResultHeaderCellViewModel -> {
                item.text
                        .bindToMain(title.text())
                        .disposedBy(item.viewDisposeBag)
            }
            is SearchResultArtworkCellViewModel -> {

            }
            is SearchResultExhibitionCellViewModel -> {

            }
            is SearchResultTourCellViewModel -> {
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
            is SearchResultArtworkCellViewModel -> 0
            is SearchResultExhibitionCellViewModel -> 0
            is SearchResultTourCellViewModel -> 0
            is SearchResultTextCellViewModel -> R.layout.layout_cell_suggested_keyword
            is SearchResultEmptyCellViewModel -> 0
            else -> R.layout.layout_cell_suggested_map_object
        }
    }

}