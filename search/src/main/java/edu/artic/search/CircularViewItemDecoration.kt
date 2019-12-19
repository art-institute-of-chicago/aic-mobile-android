package edu.artic.search

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

class CircularViewItemDecoration(private val currentPadding: Int,
                                 private val finalPadding: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val adapter = parent.adapter as SearchResultsAdapter

        adapter.getItemOrNull(position)?.let {
            if (it is OrderedCellViewModel && it.order > -1) {
                val artworkColumn = it.order % SearchResultsAdapter.MAX_ARTWORKS_PER_ROW
                val middlePadding = finalPadding - currentPadding
                when {
                    artworkColumn > 0 -> {
                        outRect.left = middlePadding * artworkColumn
                        outRect.right = 0
                    }
                    else -> {
                        outRect.left = 0
                        outRect.right = 0
                    }
                }
            }
        }
    }
}