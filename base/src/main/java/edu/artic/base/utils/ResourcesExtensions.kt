package edu.artic.base.utils

import android.content.res.Resources
import android.util.TypedValue

/**
 * Determine how many pixels a given [amount of dp][TypedValue.COMPLEX_UNIT_DIP]
 * should equal.
 */
fun Resources.dpToPixels(quantity: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, quantity, displayMetrics)
}