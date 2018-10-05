package edu.artic.search

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textRes
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.image.GlideApp
import kotlinx.android.synthetic.main.layout_cell_amenity.view.*
import kotlinx.android.synthetic.main.layout_cell_header.view.*
import kotlinx.android.synthetic.main.layout_cell_result_header.view.*
import kotlinx.android.synthetic.main.layout_cell_search_list_item.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_keyword.view.*
import kotlinx.android.synthetic.main.layout_cell_suggested_map_object.view.*

class SearchResultsAdapter : AutoHolderRecyclerViewAdapter<SearchBaseCellViewModel>() {

    companion object {
        const val MAX_ARTWORKS_PER_ROW = 5
    }

    override fun View.onBindView(item: SearchBaseCellViewModel, position: Int) {
        setTag(R.id.tag_holder,item)
        when (item) {

            is SearchHeaderCellViewModel -> {
                item.text
                        .bindToMain(title.textRes())
                        .disposedBy(item.viewDisposeBag)

                //Rx version of clicks does not work here for some reason :(
                seeAllText.setOnClickListener {
                    item.onClickSeeAll()
                }
            }

            is SearchTextHeaderViewModel -> {
                item.text
                        .bindToMain(headerText.textRes())
                        .disposedBy(item.viewDisposeBag)
            }
            is SearchBaseListItemViewModel -> {
                item.imageUrl
                        .subscribe {
                            GlideApp.with(context)
                                    .load(it)
                                    .placeholder(R.drawable.placeholder_thumb)
                                    .into(image)
                        }.disposedBy(item.viewDisposeBag)
                item.isHeadphonesVisible
                        .bindToMain(headphonesIcon.visibility(View.INVISIBLE))
                        .disposedBy(item.viewDisposeBag)

                item.itemTitle
                        .bindToMain(itemTitle.text())
                        .disposedBy(item.viewDisposeBag)
                item.itemSubTitle
                        .bindToMain(itemSubTitle.text())
                        .disposedBy(item.viewDisposeBag)

                when (item) {
                    is SearchArtworkCellViewModel -> {

                    }
                    is SearchExhibitionCellViewModel -> {

                    }
                    is SearchTourCellViewModel -> {
                    }
                }
            }
            is SearchTextCellViewModel -> {
                item.text
                        .map { (text, highlightedText) ->
                            if (highlightedText.isEmpty()) {
                                return@map text
                            } else {
                                val withSpans = SpannableString(text)

                                applyHighlight(withSpans, highlightedText)

                                return@map withSpans
                            }
                        }
                        .bindTo(suggestedKeyword.text())
                        .disposedBy(item.viewDisposeBag)
            }
            is SearchCircularCellViewModel -> {
                item.imageUrl
                        .subscribe {
                            GlideApp.with(this)
                                    .load(it)
                                    .placeholder(R.drawable.circular_placeholder)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(circularImage)
                        }
                        .disposedBy(item.viewDisposeBag)
            }
            is SearchAmenitiesCellViewModel -> {
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

    /**
     * Marks out the section of [target] matching [highlightedText] in bold.
     */
    private fun applyHighlight(target: SpannableString, highlightedText: String) {
        val startIndex = target.indexOf(string = highlightedText, ignoreCase = true)
        if (startIndex > -1) {
            target.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    startIndex + highlightedText.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun getLayoutResId(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is SearchHeaderCellViewModel -> R.layout.layout_cell_result_header
            is SearchTextHeaderViewModel -> R.layout.layout_cell_header
            is SearchBaseListItemViewModel -> R.layout.layout_cell_search_list_item
            is SearchTextCellViewModel -> R.layout.layout_cell_suggested_keyword
            is SearchEmptyCellViewModel -> R.layout.layout_cell_empty
            is SearchAmenitiesCellViewModel -> R.layout.layout_cell_amenity
            is RowPaddingViewModel -> R.layout.layout_cell_divider
            else -> R.layout.layout_cell_suggested_map_object
        }
    }


    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        val tag = holder.itemView.getTag(R.id.tag_holder)
        if (tag is SearchBaseCellViewModel) {
            tag.apply {
                cleanup()
            }
        }
    }

    fun getSpanCount(position: Int): Int {
        val cell = getItemOrNull(position)
        return if (cell is SearchCircularCellViewModel || cell is SearchAmenitiesCellViewModel) {
            1
        } else {
            MAX_ARTWORKS_PER_ROW
        }
    }
}