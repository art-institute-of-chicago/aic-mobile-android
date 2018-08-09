package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import de.hdodenhof.circleimageview.CircleImageView
import edu.artic.map.R


class ArticObjectDotMarkerGenerator(context: Context) : BaseMarkerGenerator(context) {

    init {
        container = LayoutInflater.from(context)
                .inflate(R.layout.marker_artic_object_dot, null) as ViewGroup
    }
}