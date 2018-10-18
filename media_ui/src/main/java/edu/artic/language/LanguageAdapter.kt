package edu.artic.language

import android.graphics.Color
import android.view.View
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.adapter.DropDownAdapter
import edu.artic.localization.SpecifiesLanguage
import edu.artic.media.ui.R
import kotlinx.android.synthetic.main.language_cell.view.*

/**
 * List adapter for the language-selection dropdown.
 *
 * This is also responsible for creating the view seen at the top
 * of the list (i.e. the 'currently selected' language).
 */
class LanguageAdapter : AutoHolderRecyclerViewAdapter<SpecifiesLanguage>(),
        DropDownAdapter<SpecifiesLanguage, BaseViewHolder> {
    override fun View.onBindView(item: SpecifiesLanguage, position: Int) {
        text.text = item.userFriendlyLanguage(context)
        text.setTextColor(Color.WHITE)
    }

    override fun getLayoutResId(position: Int): Int = R.layout.language_cell

    override fun View.onBindDropdownView(item: SpecifiesLanguage, position: Int) {
        text.text = item.userFriendlyLanguage(context)
    }
}