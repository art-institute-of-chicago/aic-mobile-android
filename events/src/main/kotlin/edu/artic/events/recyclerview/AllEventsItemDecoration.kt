package edu.artic.events.recyclerview

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.events.AllEventsAdapter
import edu.artic.events.AllEventsCellHeaderViewModel
import edu.artic.events.AllEventsCellViewModel
import edu.artic.events.R

class AllEventsItemDecoration(
        context: Context,
        private val spanCount: Int,
        private val adapter: AllEventsAdapter
) : RecyclerView.ItemDecoration() {
    private val horizontalSpacing: Int = context.resources.getDimension(R.dimen.all_tour_cell_spacing_horizontal).toInt()
    private val verticalSpacing: Int = context.resources.getDimension(R.dimen.all_tour_cell_spacing_vertical).toInt()
    private val headerVerticalSpacing: Int = context.resources.getDimension(R.dimen.all_tour_cell_spacing_vertical_header).toInt()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        var position = parent.getChildAdapterPosition(view) // item position
        adapter.getItemOrNull(position)?.let {
            if (it is AllEventsCellHeaderViewModel) {
                outRect.left = horizontalSpacing
                outRect.right = horizontalSpacing
                outRect.top = if (position == 0) headerVerticalSpacing else verticalSpacing
                outRect.bottom = (verticalSpacing / 2.0f).toInt()
            } else if (it is AllEventsCellViewModel) {
                position -= it.headerPosition - 1
                val column = (position) % spanCount // item column
                outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * horizontalSpacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)
                outRect.top = (verticalSpacing / 2.0f).toInt()
                outRect.bottom = (verticalSpacing / 2.0f).toInt()
            }
        }

    }
}