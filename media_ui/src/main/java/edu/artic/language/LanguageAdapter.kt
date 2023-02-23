package edu.artic.language

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.adapter.DropDownAdapter
import edu.artic.localization.SpecifiesLanguage
import edu.artic.media.ui.R
import edu.artic.media.ui.databinding.DropdownLanguageCellBinding
import edu.artic.media.ui.databinding.LanguageCellBinding


/**
 * List adapter for the language-selection dropdown.
 *
 * This is also responsible for creating the view seen at the top
 * of the list (i.e. the 'currently selected' language).
 */
class LanguageAdapter :
    AutoHolderRecyclerViewAdapter<LanguageCellBinding, SpecifiesLanguage>(),
    DropDownAdapter<SpecifiesLanguage, BaseViewHolder> {
    override fun View.onBindView(item: SpecifiesLanguage, holder: BaseViewHolder, position: Int) {
        val binding = holder.binding as LanguageCellBinding
        binding.text.text = item.userFriendlyLanguage(context)
        binding.text.setTextColor(Color.WHITE)
    }

    override fun getLayoutResId(position: Int): Int = R.layout.dropdown_language_cell

    override fun View.onBindDropdownView(
        item: SpecifiesLanguage,
        baseViewHolder: BaseViewHolder,
        position: Int,
    ) {
        val binding = baseViewHolder.binding as DropdownLanguageCellBinding
        binding.text.text = item.userFriendlyLanguage(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

        return ContentViewHolder(
            LanguageCellBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), R.layout.language_cell
        )
    }

    override fun onCreateDropdownItemViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

        return DropDownViewHolder(
            DropdownLanguageCellBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), getLayoutResId(0)
        )
    }

    class DropDownViewHolder(binding: ViewBinding, layout: Int) :
        BaseViewHolder(binding, layout) {

    }

    class ContentViewHolder(binding: ViewBinding, layout: Int) : BaseViewHolder(binding, layout)
}