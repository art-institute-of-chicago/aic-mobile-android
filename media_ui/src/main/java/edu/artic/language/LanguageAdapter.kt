package edu.artic.language

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.adapter.DropDownAdapter
import edu.artic.localization.SpecifiesLanguage
import edu.artic.media.ui.R
import kotlinx.android.synthetic.main.dropdown_language_cell.view.*

/**
 * List adapter for the language-selection dropdown.
 *
 * This is also responsible for creating the view seen at the top
 * of the list (i.e. the 'currently selected' language).
 */
// TODO: See if `:media_ui` is still the best module for this file
class LanguageAdapter : AutoHolderRecyclerViewAdapter<SpecifiesLanguage>(),
        DropDownAdapter<SpecifiesLanguage, BaseViewHolder> {
    override fun View.onBindView(item: SpecifiesLanguage, position: Int) {
        text.text = item.userFriendlyLanguage(context)
        text.setTextColor(Color.WHITE)
    }

    override fun getLayoutResId(position: Int): Int = R.layout.dropdown_language_cell

    override fun View.onBindDropdownView(item: SpecifiesLanguage, position: Int) {
        text.text = item.userFriendlyLanguage(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return ContentViewHolder(parent, R.layout.language_cell)
    }

    override fun onCreateDropdownItemViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder? {
        return DropDownViewHolder(parent, getLayoutResId(0))
    }

    class DropDownViewHolder(viewGroup: ViewGroup, layout: Int) : BaseViewHolder(viewGroup, layout)

    class ContentViewHolder(viewGroup: ViewGroup, layout: Int) : BaseViewHolder(viewGroup, layout)
}