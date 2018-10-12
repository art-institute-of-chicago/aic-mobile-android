package edu.artic.view

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.design.widget.AppBarLayout
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.jakewharton.rxbinding2.widget.text
import edu.artic.ui.R
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.view_detail_button_layout.view.*

/**
 * Description: Wraps the layout and common functionality for the main collapsing [AppBarLayout] in
 * this app.
 */
class DetailButton(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_detail_button_layout, this)

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.DetailButton,
                    0, 0
            )
            setIcon(a.getResourceId(R.styleable.DetailButton_button_icon, 0))
            buttonText.text = a.getString(R.styleable.DetailButton_button_text)
        }
    }

    fun setIcon(@DrawableRes iconId: Int) {
        buttonText.setCompoundDrawables(resources.getDrawable(iconId, context.theme),null,null,null)
    }

    fun text(): Consumer<in CharSequence> {
        return buttonText.text()
    }

}