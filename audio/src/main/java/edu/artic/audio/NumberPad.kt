package edu.artic.audio

import android.support.annotation.LayoutRes
import android.view.ViewGroup
import android.widget.TextView
import edu.artic.adapter.BaseRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder


/**
 * Internal common holder for views which display [NumberPadElement]s.
 */
open class NumberPadViewHolder(parent: ViewGroup, resId: Int) : BaseViewHolder(parent, resId)

/**
 * One of the elements in this NumberPad. This is what distinguishes the various
 * buttons.
 */
sealed class NumberPadElement {
    data class Numeric(val value: String) : NumberPadElement()

    object DeleteBack : NumberPadElement()

    object GoSearch : NumberPadElement()
}


class CircularElementViewHolder(parent: ViewGroup, @LayoutRes layout: Int): NumberPadViewHolder(parent, layout) {

    val number : TextView = itemView.findViewById(R.id.number_content)
}


class DeleteBackViewHolder(parent: ViewGroup, @LayoutRes layout: Int): NumberPadViewHolder(parent, layout) {

}

/**
 * Layout within RecyclerView (left-to-right, top-to-bottom):
 *
 *     1  2  3
 *     4  5  6
 *     7  8  9
 *     B  0  G
 *
 * Where `B` means 'Delete back one character' and `G` means `Search with this input`.
 */
class NumberPadAdapter : BaseRecyclerViewAdapter<NumberPadElement, NumberPadViewHolder>() {
    override fun getViewType(position: Int): Int {
        val element = getItem(position)

        return when (element) {
            is NumberPadElement.Numeric -> 0
            NumberPadElement.DeleteBack -> 1
            NumberPadElement.GoSearch -> 2
        }
    }

    override fun getLayoutResId(position: Int): Int {
        // We don't expect this method to get called because this class also overrides .getViewType
        TODO("NumberPadAdapter.getLayoutResId is not supported at this time.")
    }

    override fun onBindViewHolder(holder: NumberPadViewHolder, item: NumberPadElement?, position: Int) {
        if (holder is CircularElementViewHolder) {
            if (item is NumberPadElement.Numeric) {
                holder.number.text = item.value
            } else if (item is NumberPadElement.GoSearch) {
                holder.number.text = "Go"
            }
        }
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): NumberPadViewHolder {
        return if (viewType == 1) {
            // 'DeleteBack' type
            DeleteBackViewHolder(parent, R.layout.view_number_pad_delete_back_element)
        } else {
            // 'Numeric' and 'GoSearch' types
            CircularElementViewHolder(parent, R.layout.view_number_pad_numeric_element)
        }
    }

}