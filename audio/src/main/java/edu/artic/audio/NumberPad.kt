package edu.artic.audio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.audio.databinding.ViewNumberPadDeleteBackElementBinding
import edu.artic.audio.databinding.ViewNumberPadNumericElementBinding

//import kotlinx.android.synthetic.main.view_number_pad_numeric_element.view.*


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
 *
 * We have 2 ViewTypes (i.e. two different layouts) and 3 different [NumberPadElement]s.
 * This means that [getLayoutResId] will return one of two resIds and that
 * [onBindView] will use one of 3 bind algorithms.
 */
class NumberPadAdapter : AutoHolderRecyclerViewAdapter<ViewBinding, NumberPadElement>() {
    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (viewType) {
            R.layout.view_number_pad_delete_back_element -> ViewNumberPadDeleteBackElementBinding.inflate(
                inflater,
                parent,
                false
            )
            else -> ViewNumberPadNumericElementBinding.inflate(
                inflater,
                parent,
                false
            )
        }
        return BaseViewHolder(binding, viewType).apply {
            itemView.onHolderCreated(parent, viewType)
        }

    }

    override fun getLayoutResId(position: Int): Int {
        return if (getItem(position) is NumberPadElement.DeleteBack) {
            // 'DeleteBack' type
            R.layout.view_number_pad_delete_back_element
        } else {
            // 'Numeric' and 'GoSearch' types
            R.layout.view_number_pad_numeric_element
        }
    }

    override fun View.onBindView(item: NumberPadElement, holder: BaseViewHolder, position: Int) {
        when (item) {
            is NumberPadElement.Numeric -> {
                val binding = holder.binding as ViewNumberPadNumericElementBinding
                binding.numberContent.text = item.value
            }
            is NumberPadElement.GoSearch -> {
                val binding = holder.binding as ViewNumberPadNumericElementBinding
                binding.numberContent.text =
                    context.getString(R.string.number_pad_go_action)
            }
            else -> {
                // Other types of NumberPadElements do not need to be bound.
            }
        }
    }

}