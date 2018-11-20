package edu.artic.search

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy

class CircularViewItemDecoration(private val currentPadding: Int,
                                 private val finalPadding: Int,
                                 private val disposeBag: DisposeBag) : RecyclerView.ItemDecoration() {

    private var onTheMapPosition = 0

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val adapter = parent.adapter as SearchResultsAdapter

        adapter.getItemOrNull(position)?.let {
            if (it is SearchTextHeaderViewModel) {
                it.text.subscribe {
                    if (it == R.string.on_the_map) {
                        onTheMapPosition = position
                    }
                }.disposedBy(disposeBag)
            } else if (onTheMapPosition > 0) {
                if (it is SearchCircularCellViewModel || it is SearchAmenitiesCellViewModel) {
                    val offsetPositionFromOnTheMap = position - (onTheMapPosition + 1)
                    val artworkColumn = offsetPositionFromOnTheMap % SearchResultsAdapter.MAX_ARTWORKS_PER_ROW
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
                } else {

                }
            } else {

            }
        }
    }

}