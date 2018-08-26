package edu.artic.audio

import android.graphics.Color
import android.view.View
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.adapter.DropDownAdapter
import edu.artic.db.models.AudioFileModel
import kotlinx.android.synthetic.main.view_language_box.view.*

/**
 * List adapter for the language-selection dropdown.
 *
 * This is also responsible for creating the view seen at the top
 * of the list (i.e. the 'currently selected' language).
 */
class LanguageAdapter : AutoHolderRecyclerViewAdapter<AudioFileModel>(),
        DropDownAdapter<AudioFileModel, BaseViewHolder> {
    override fun View.onBindView(item: AudioFileModel, position: Int) {
        text.text = item.userFriendlyLanguage(context)
        text.setTextColor(Color.WHITE)
    }

    override fun getLayoutResId(position: Int): Int = R.layout.view_language_box

    override fun View.onBindDropdownView(item: AudioFileModel, position: Int) {
        text.text = item.userFriendlyLanguage(context)
    }
}