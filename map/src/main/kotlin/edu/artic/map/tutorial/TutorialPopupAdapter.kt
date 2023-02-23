package edu.artic.map.tutorial

import android.view.View
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.map.R
import edu.artic.map.databinding.TutorialPopupItemBinding

class TutorialPopupAdapter : AutoHolderRecyclerViewAdapter<TutorialPopupItemViewModel>() {

    override fun View.onBindView(
        item: TutorialPopupItemViewModel,
        holder: BaseViewHolder,
        position: Int
    ) {
        with(holder.binding as TutorialPopupItemBinding) {
            image.setImageResource(item.imageId)
            popupText.setText(item.textId)
        }
    }

    override fun getLayoutResId(position: Int): Int = R.layout.tutorial_popup_item
}