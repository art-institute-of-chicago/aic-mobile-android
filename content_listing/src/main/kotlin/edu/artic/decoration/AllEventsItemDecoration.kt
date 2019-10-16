package edu.artic.decoration

import android.content.res.Resources
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.content.listing.R
import edu.artic.events.AllEventsAdapter
import edu.artic.events.AllEventsCellHeaderViewModel
import edu.artic.events.EventCellViewModel

/**
 * The original version of this file was written by
 * [edwardaa](https://stackoverflow.com/users/3414249/edwardaa) for
 * [https://stackoverflow.com/a/30701422](https://stackoverflow.com/a/30701422).
 */
class AllEventsItemDecoration(
        override val spanCount: Int,
        private val adapter: AllEventsAdapter
) : GridItemDecoration(spanCount) {

    override fun createDimensions(res: Resources): Dimensions {
        return object : Dimensions {
            override val horizontal: Int = res.getDimensionPixelOffset(R.dimen.all_tour_cell_spacing_horizontal)
            override val vertical: Int = res.getDimensionPixelOffset(R.dimen.all_tour_cell_spacing_vertical)
            override val topMostVertical: Int = res.getDimensionPixelOffset(R.dimen.all_tour_cell_spacing_vertical_header)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val horizontalSpacing = dimensions.horizontal
        val verticalSpacing = dimensions.vertical
        val headerVerticalSpacing = dimensions.topMostVertical

        // item position
        val position = parent.getChildAdapterPosition(view)
        adapter.getItemOrNull(position)?.let {

            val halfOfVertical = dimensions.halfOfVertical()

            when (it) {
                is AllEventsCellHeaderViewModel -> {
                    outRect.left = horizontalSpacing
                    outRect.right = horizontalSpacing
                    outRect.top = if (position == 0) headerVerticalSpacing else verticalSpacing
                    outRect.bottom = halfOfVertical
                }
                is EventCellViewModel -> {
                    val adjustedPosition = position - (it.headerPosition - 1)
                    val column = (adjustedPosition) % spanCount // item column
                    outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
                    outRect.right = (column + 1) * horizontalSpacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)
                    outRect.top = halfOfVertical
                    outRect.bottom = halfOfVertical
                }
            }
        }

    }
}
