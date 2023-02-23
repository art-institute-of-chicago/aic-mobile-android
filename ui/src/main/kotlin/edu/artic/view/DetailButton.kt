package edu.artic.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.google.android.material.appbar.AppBarLayout
import com.jakewharton.rxbinding2.widget.text
import edu.artic.ui.R
import edu.artic.ui.databinding.ViewDetailButtonLayoutBinding
import io.reactivex.functions.Consumer

//import kotlinx.android.synthetic.main.view_detail_button_layout.view.*

/**
 * Description: Wraps the layout and common functionality for the main collapsing [AppBarLayout] in
 * this app.
 */
class DetailButton(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs, 0, R.style.DetailButtonParent) {

    private val binding: ViewDetailButtonLayoutBinding

    init {
        binding =
            ViewDetailButtonLayoutBinding.inflate(LayoutInflater.from(context), this)

        View.inflate(context, R.layout.view_detail_button_layout, this)

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.DetailButton,
                0, R.style.DetailButtonParent
            )
            setIcon(a.getResourceId(R.styleable.DetailButton_button_icon, 0))
            binding.buttonText.text = a.getString(R.styleable.DetailButton_button_text)
        }
    }

    fun setIcon(@DrawableRes iconId: Int) {
        binding.buttonText.setCompoundDrawablesRelativeWithIntrinsicBounds(iconId, 0, 0, 0)
    }

    fun text(): Consumer<in CharSequence> {
        return binding.buttonText.text()
    }

}