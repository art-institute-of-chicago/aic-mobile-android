package edu.artic.map.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import edu.artic.map.R


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