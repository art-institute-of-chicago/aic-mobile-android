package edu.artic.decoration

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.content.listing.R

/**
 * The original version of this file was written by
 * [edwardaa](https://stackoverflow.com/users/3414249/edwardaa) for
 * [https://stackoverflow.com/a/30701422](https://stackoverflow.com/a/30701422).
 */
class AllExhibitionsItemDecoration(
        context: Context,
        override val spanCount: Int
) : GridItemDecoration(spanCount) {

    override fun createDimensions(res: Resources): Dimensions {
        return object : Dimensions {
            override val horizontal: Int = res.getDimensionPixelSize(R.dimen.all_exhibitions_cell_spacing_horizontal)
            override val vertical: Int = res.getDimensionPixelSize(R.dimen.all_exhibitions_cell_spacing_vertical)
            override val topMostVertical: Int = vertical
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val horizontalSpacing = dimensions.horizontal
        val verticalSpacing = dimensions.vertical

        val adjustedPos = parent.getChildAdapterPosition(view) - 1 // item position, minus 1 for the header
        val column = (adjustedPos) % spanCount // item column

        outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
        outRect.right = (column + 1) * horizontalSpacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

        if (adjustedPos < spanCount) { // top edge
            outRect.top = verticalSpacing
        }
        outRect.bottom = verticalSpacing // item bottom

    }
}