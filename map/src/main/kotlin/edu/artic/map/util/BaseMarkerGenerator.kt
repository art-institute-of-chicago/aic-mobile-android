package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup


/**
 * Simple [Bitmap]-generating class for drawing map markers from custom layouts.
 *
 * If you're familiar with RecyclerView's
 * [ViewHolder][android.support.v7.widget.RecyclerView.ViewHolder], the concept
 * is pretty similar. Google Maps API v2 doesn't readily support all the custom
 * appearance you could get through Android's View SDKs, so we built out this
 * class to adapt stylish layouts into the [Bitmap] form it can manage.
 *
 * Inflate (or assign) your custom layout to [BaseMarkerGenerator.container]
 * in the constructor or `init` block, and retrieve a new Bitmap whenever
 * you need one with [BaseMarkerGenerator.makeIcon].
 */
open class BaseMarkerGenerator(val context: Context) {
    /**
     * Parent layout, containing the entirety of the marker's visuals.
     *
     * Subclasses should assign a value to this in their `init` blocks. To
     * prevent any layout issues that [makeIcon] might trigger, we recommend
     * keeping [container] detached from any and all [parents][View.getParent].
     */
    lateinit var container: ViewGroup


    /**
     * Draw the marker represented by [container] onto a new Bitmap-backed Canvas.
     *
     * Keep in mind that this _can_ trigger an [OutOfMemoryError] if the layout is
     * too big, so be careful.
     *
     * *Advanced Note* This method calls [View.measure] and [View.draw]. If
     * [container] is [attached to a Window][View.getWindowToken] at the time, the
     * Window might re-layout itself.
     */
    fun makeIcon(): Bitmap {
        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        container.measure(measureSpec, measureSpec)

        val measuredWidth = container.measuredWidth
        val measuredHeight = container.measuredHeight

        container.layout(0, 0, measuredWidth, measuredHeight)

        val r = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        r.eraseColor(Color.TRANSPARENT)

        val canvas = Canvas(r)
        container.draw(canvas)
        return r
    }
}