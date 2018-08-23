package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import edu.artic.map.R


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