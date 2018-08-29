package edu.artic.audio

import android.view.ViewGroup
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
    class Numeric(value: String)

    class DeleteBack()

    class GoSearch()
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