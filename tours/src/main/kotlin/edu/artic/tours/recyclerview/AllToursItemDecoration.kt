package edu.artic.tours.recyclerview

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.tours.R

class AllToursItemDecoration(
        context: Context,
        private val spanCount: Int,
        private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {
    private val horizontalSpacing: Int = context.resources.getDimension(R.dimen.all_tour_cell_spacing_horizontal).toInt()
    private val verticalSpacing: Int = context.resources.getDimension(R.dimen.all_tour_cell_spacing_vertical).toInt()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        val position = parent.getChildAdapterPosition(view) // item position
        val column = position % spanCount // item column

        if (includeEdge) {
            outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (column + 1) * horizontalSpacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

            if (position < spanCount) { // top edge
                outRect.top = verticalSpacing
            }
            outRect.bottom = verticalSpacing // item bottom
        } else {
            outRect.left = column * horizontalSpacing / spanCount // column * ((1f / spanCount) * spacing)
            outRect.right = horizontalSpacing - (column + 1) * horizontalSpacing / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
            if (position >= spanCount) {
                outRect.top = verticalSpacing // item top
            }
        }
    }
}