package edu.artic.map.tutorial

import android.view.View
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.map.R
import kotlinx.android.synthetic.main.tutorial_popup_item.view.*


class TutorialPopupAdapter : AutoHolderRecyclerViewAdapter<TutorialPopupItemViewModel>() {

    override fun View.onBindView(item: TutorialPopupItemViewModel, position: Int) {
        image.setImageResource(item.imageId)
        popupText.setText(item.textId)
    }

    override fun getLayoutResId(position: Int): Int = R.layout.tutorial_popup_item
}