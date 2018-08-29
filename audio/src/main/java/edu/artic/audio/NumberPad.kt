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
    override fun getLayoutResId(position: Int): Int {
        TODO("not implemented")
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): NumberPadViewHolder {
        TODO("not implemented")
    }

    override fun onBindViewHolder(holder: NumberPadViewHolder, item: NumberPadElement?, position: Int) {
        TODO("not implemented")
    }

}