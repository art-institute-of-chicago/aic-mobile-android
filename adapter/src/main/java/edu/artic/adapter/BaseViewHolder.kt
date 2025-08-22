package edu.artic.adapter

import android.content.Context
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Description: ViewHolder that holds a required bindingSafe.
 */
open class BaseViewHolder(
    val binding: ViewBinding,
    // Left for backward capability with old source codes
    // And the simpler migration should be removed in the future
    @LayoutRes val layout: Int,
) : RecyclerView.ViewHolder(binding.root) {

    val context: Context
        get() = itemView.context
}
