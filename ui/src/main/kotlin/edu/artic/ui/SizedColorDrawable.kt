package edu.artic.ui

import android.graphics.drawable.ColorDrawable
import android.support.annotation.ColorInt

/**
 * Simple extension to [ColorDrawable] with configurable height and width.
 *
 * Dimensions must be supplied in pixels.
 * @see edu.artic.base.utils.dpToPixels
 */
class SizedColorDrawable(
        @ColorInt color: Int,
        private val height: Int = -1,
        private val width: Int = -1
) : ColorDrawable(color) {

    override fun getIntrinsicHeight(): Int {
        return height
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }
}
