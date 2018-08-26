package edu.artic.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

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

class ArticObjectDotMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_artic_object_dot, null) as ViewGroup
    }
}

class ArticObjectMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    /**
     * Where the [icon][BaseMarkerGenerator.makeIcon] will appear.
     */
    private val imageView: ImageView
    private val overlayTextView: TextView

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_artic_object, null) as ViewGroup
        imageView = container.findViewById(R.id.circularImage)
        overlayTextView = container.findViewById(R.id.order)
    }


    /**
     * NB: 'imageViewBitmap' _MUST_ be rendered in software. If its config
     * is [android.graphics.Bitmap.Config.HARDWARE], the
     * [CircleImageView][de.hdodenhof.circleimageview.CircleImageView]
     * may crash.
     */
    fun makeIcon(imageViewBitmap: Bitmap, overlay: String? = null): Bitmap {

        imageView.setImageBitmap(imageViewBitmap)

        if (overlay != null) {
            overlayTextView.visibility = View.VISIBLE
            overlayTextView.text = overlay
        } else {
            overlayTextView.visibility = View.GONE
        }
        return makeIcon()
    }
}

class DepartmentMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_department, null) as ViewGroup
    }


    fun makeIcon(bitmap: Bitmap, charSequence: CharSequence): Bitmap {
        container.findViewById<ImageView>(R.id.departmentImage).setImageBitmap(bitmap)
        container.findViewById<TextView>(R.id.text).text = charSequence
        return makeIcon()
    }
}

class GalleryNumberMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_gallery_number, null) as ViewGroup
    }


    fun makeIcon(charSequence: CharSequence): Bitmap {
        container.findViewById<TextView>(R.id.text).text = charSequence
        return makeIcon()
    }
}