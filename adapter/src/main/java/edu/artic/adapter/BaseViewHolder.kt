package edu.artic.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

/**
 * Description: ViewHolder that holds a required bindingSafe.
 */
open class BaseViewHolder(
    viewGroup: ViewGroup,
    @LayoutRes
    val layout: Int,
) :
    RecyclerView.ViewHolder(
        LayoutInflater.from(viewGroup.context)
            .inflate(layout, viewGroup, false)
    ) {

    val context: Context
        get() = itemView.context
}
