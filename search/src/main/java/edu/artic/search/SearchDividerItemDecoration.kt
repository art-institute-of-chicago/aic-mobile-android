package edu.artic.search

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import edu.artic.base.utils.dpToPixels

class SearchDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val mPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.warmGrey20Alpha)
    }
    private val mDividerHeight: Int = context.resources.dpToPixels(2f).toInt()
    private val mBottomPadding: Int = context.resources.dpToPixels(20f).toInt()


    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        val adapter = parent.adapter as SearchResultsAdapter
        for (i in 0 until parent.childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            adapter.getItemOrNull(position)?.let {
                if ((it is SearchTextCellViewModel || it.hasDivider) && (position < adapter.itemCount - 1 )) {
                    c.drawRect(
                            view.left.toFloat(),
                            view.bottom.toFloat() + mBottomPadding,
                            view.right.toFloat(),
                            view.bottom + mBottomPadding + mDividerHeight.toFloat(),
                            mPaint
                    )
                } else if ((it is SearchBaseListItemViewModel || it.hasDivider) && (position < adapter.itemCount - 1 )) {
                    c.drawRect(
                            view.left.toFloat(),
                            view.bottom.toFloat(),
                            view.right.toFloat(),
                            view.bottom + mDividerHeight.toFloat(),
                            mPaint
                    )
                } else {
                }
            }
        }

    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val adapter = parent.adapter as SearchResultsAdapter
        adapter.getItemOrNull(position)?.let {
            if ((it is SearchTextCellViewModel || it.hasDivider) && (position < adapter.itemCount - 1 )) {
                outRect.set(0, 0, 0, mBottomPadding+mDividerHeight)
            } else if ((it is SearchBaseListItemViewModel || it.hasDivider) && (position < adapter.itemCount - 1 )) {
                outRect.set(0, 0, 0, mDividerHeight)
            } else {
                outRect.set(0, 0, 0, 0)
            }
        }
    }

}