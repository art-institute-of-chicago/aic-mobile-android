package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.hdodenhof.circleimageview.CircleImageView
import edu.artic.map.R


open class BaseMarkerGenerator(val context: Context) {
    lateinit var container: ViewGroup


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