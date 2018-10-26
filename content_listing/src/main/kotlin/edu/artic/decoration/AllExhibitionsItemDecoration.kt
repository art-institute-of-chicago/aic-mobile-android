package edu.artic.decoration

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.content.listing.R

class AllExhibitionsItemDecoration(
        context: Context,
        private val spanCount: Int,
        private val includeEdge: Boolean = true
) : RecyclerView.ItemDecoration() {
    private val horizontalSpacing: Int = context.resources.getDimensionPixelSize(R.dimen.all_exhibitions_cell_spacing_horizontal)
    private val verticalSpacing: Int = context.resources.getDimensionPixelSize(R.dimen.all_exhibitions_cell_spacing_vertical)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adjustedPos = parent.getChildAdapterPosition(view) - 1 // item position, minus 1 for the header
        val column = (adjustedPos) % spanCount // item column
        if (includeEdge) {
            outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (column + 1) * horizontalSpacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

            if (adjustedPos < spanCount) { // top edge
                outRect.top = verticalSpacing
            }
            outRect.bottom = verticalSpacing // item bottom
        } else {
            outRect.left = column * horizontalSpacing / spanCount // column * ((1f / spanCount) * spacing)
            outRect.right = horizontalSpacing - (column + 1) * horizontalSpacing / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
            if (adjustedPos >= spanCount) {
                outRect.top = verticalSpacing // item top
            }
        }

    }
}