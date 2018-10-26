package edu.artic.decoration

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.content.listing.R

/**
 * The original version of this file was written by
 * [edwardaa](https://stackoverflow.com/users/3414249/edwardaa) for
 * [https://stackoverflow.com/a/30701422](https://stackoverflow.com/a/30701422).
 */
class AllToursItemDecoration(
        context: Context,
        private val spanCount: Int
) : RecyclerView.ItemDecoration() {
    private val horizontalSpacing: Int = context.resources.getDimensionPixelSize(R.dimen.all_tour_cell_spacing_horizontal)
    private val verticalSpacing: Int = context.resources.getDimensionPixelSize(R.dimen.all_tour_cell_spacing_vertical)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adjustedPos = parent.getChildAdapterPosition(view) - 1 // item position, minus 1 for the header
        if (adjustedPos < 0) {
            outRect.set(0, 0, 0, 0)
        } else {
            val column = (adjustedPos) % spanCount // item column

            outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (column + 1) * horizontalSpacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

            if (adjustedPos < spanCount) { // top edge
                outRect.top = verticalSpacing
            }
            outRect.bottom = verticalSpacing // item bottom
        }

    }
}