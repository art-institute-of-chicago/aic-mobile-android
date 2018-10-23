package edu.artic.map

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet


/**
 * WrapContentHeightViewPager updates it's height to the height of the biggest child it currently has.
 * @see http://stackoverflow.com/questions/8394681/android-i-am-unable-to-have-viewpager-wrap-content/20784791#20784791
 * @author Sameer Dhakal (Fuzz)
 */
class WrapContentHeightViewPager(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newHeightMeasureSpec: Int
        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val h = child.measuredHeight
            if (h > height) height = h
        }

        newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }
}