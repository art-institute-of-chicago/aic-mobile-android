package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import edu.artic.map.R


class ArticObjectMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_artic_object, null) as ViewGroup
    }


    fun makeIcon(imageViewBitmap: Bitmap, order: String? = null): Bitmap {
        container.findViewById<CircleImageView>(R.id.circularImage).setImageBitmap(imageViewBitmap)
        order?.let {
            container.findViewById<TextView>(R.id.order).text = it
        }
        return makeIcon()
    }
}