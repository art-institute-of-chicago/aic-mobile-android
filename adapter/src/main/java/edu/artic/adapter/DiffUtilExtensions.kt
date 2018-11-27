package edu.artic.adapter

import android.support.v7.util.DiffUtil

/**
 * @author Sameer Dhakal (Fuzz)
 */

fun <T> getDefaultDiffCallback(): DiffUtil.ItemCallback<T> {

    return object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
                areContentsTheSame(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
                oldItem == newItem
    }
}