package edu.artic.events.recyclerview

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.content.listing.R
import edu.artic.events.AllEventsAdapter
import edu.artic.events.AllEventsCellHeaderViewModel
import edu.artic.events.EventCellViewModel

class AllEventsItemDecoration(
        context: Context,
        private val spanCount: Int,
        private val adapter: AllEventsAdapter
) : RecyclerView.ItemDecoration() {
    private val horizontalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.all_tour_cell_spacing_horizontal)
    private val verticalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.all_tour_cell_spacing_vertical)
    private val headerVerticalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.all_tour_cell_spacing_vertical_header)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        // item position
        val position = parent.getChildAdapterPosition(view)
        adapter.getItemOrNull(position)?.let {

            val halfOfVertical = (verticalSpacing / 2.0f).toInt()

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
