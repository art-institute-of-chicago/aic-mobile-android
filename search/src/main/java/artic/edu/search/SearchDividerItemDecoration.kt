package artic.edu.search

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View

class SearchDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val mPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.warmGrey20Alpha)
    }
    private val mDividerHeight: Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            context.resources.displayMetrics).toInt()


    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        val adapter = parent.adapter as SearchResultsAdapter
        for (i in 0 until parent.childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            adapter.getItemOrNull(position)?.let {
                if (it is SearchBaseListItemViewModel || it.hasDivider) {
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
            if (it is SearchBaseListItemViewModel || it.hasDivider) {
                outRect.set(0, 0, 0, mDividerHeight)
            } else {
                outRect.set(0, 0, 0, 0)
            }
        }
    }

}