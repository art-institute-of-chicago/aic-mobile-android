package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import edu.artic.map.R


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