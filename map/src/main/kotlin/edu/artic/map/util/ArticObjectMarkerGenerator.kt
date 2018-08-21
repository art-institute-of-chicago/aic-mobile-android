package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import edu.artic.map.R


class ArticObjectMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    private var imageView: CircleImageView

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_artic_object, null) as ViewGroup
         imageView = container.findViewById(R.id.circularImage)
    }


    fun makeIcon(imageViewBitmap: Bitmap, order: String? = null): Bitmap {

        imageView.setImageBitmap(imageViewBitmap)

        val orderTextView = container.findViewById<TextView>(R.id.order)
        if (order != null) {
            orderTextView.visibility = View.VISIBLE
            orderTextView.text = order
        } else {
            orderTextView.visibility = View.GONE
        }
        return makeIcon()
    }
}