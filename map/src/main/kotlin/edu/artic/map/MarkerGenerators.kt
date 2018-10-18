package edu.artic.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import edu.artic.base.utils.dpToPixels

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
    fun makeIcon(scale: Float = 1f): Bitmap {
        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        container.measure(measureSpec, measureSpec)

        val w = container.measuredWidth
        val h = container.measuredHeight

        container.layout(0, 0, w, h)

        val r = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        r.eraseColor(Color.TRANSPARENT)

        val canvas = Canvas(r)
        container.draw(canvas)

        if (scale < 1f) {
            // Draw that Bitmap into a second Bitmap.
            val second = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
            val scaled = Canvas(second)
            scaled.drawBitmap(
                    r,
                    Matrix().apply { preScale(scale, scale) },
                    null
            )
            return second
        }

        return r
    }
}

/**
 * Used for displaying non-focused objects on the map.
 */
class ArticObjectDotMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_artic_object_dot, null) as ViewGroup
    }
}

/**
 * Used for displaying [ArticObject] on the map.
 */
class ArticObjectMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    /**
     * Where the [icon][BaseMarkerGenerator.makeIcon] will appear.
     */
    private val imageView: CircleImageView
    private val overlayTextView: TextView
    private val baseView: View
    private val defaultImage: ColorDrawable

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_artic_object, null) as ViewGroup
        imageView = container.findViewById(R.id.circularImage)
        overlayTextView = container.findViewById(R.id.order)
        baseView = container.findViewById(R.id.base)
        defaultImage = ColorDrawable(ContextCompat.getColor(context, R.color.map_light_dot))
    }


    /**
     * NB: 'imageViewBitmap' _MUST_ be rendered in software. If its config
     * is [android.graphics.Bitmap.Config.HARDWARE], the
     * [CircleImageView][de.hdodenhof.circleimageview.CircleImageView]
     * may crash.
     */
    fun makeIcon(imageViewBitmap: Bitmap?, scale: Float = 1f, overlay: String? = null, selected: Boolean = false): Bitmap {

        if (selected) {
            imageView.borderWidth = imageView.resources.dpToPixels(4f).toInt()
        } else {
            imageView.borderWidth = imageView.resources.dpToPixels(2f).toInt()
        }

        if (imageViewBitmap != null) {
            imageView.setImageBitmap(imageViewBitmap)
        } else {
            imageView.setImageDrawable(defaultImage)
        }

        if (overlay != null) {
            overlayTextView.visibility = View.VISIBLE
            overlayTextView.text = overlay
        } else {
            overlayTextView.visibility = View.GONE
        }

        if (scale < 1f) {
            baseView.visibility = View.GONE
        } else {
            baseView.visibility = View.VISIBLE
        }

        return makeIcon(scale)
    }
}

/**
 * Used for displaying departments on the map.
 */
class DepartmentMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.view_marker_department, null) as ViewGroup
    }


    fun makeIcon(bitmap: Bitmap, charSequence: CharSequence): Bitmap {
        container.findViewById<ImageView>(R.id.departmentImage).setImageBitmap(bitmap)
        container.findViewById<TextView>(R.id.text).text = charSequence
        return makeIcon()
    }
}

/**
 * Generates marker only displaying text.
 */
class TextMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.view_marker_text, null) as ViewGroup
    }


    fun makeIcon(charSequence: CharSequence): Bitmap {
        container.findViewById<TextView>(R.id.text).text = charSequence
        return makeIcon()
    }
}