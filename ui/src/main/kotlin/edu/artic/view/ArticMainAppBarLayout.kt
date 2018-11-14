package edu.artic.view

import android.content.Context
import android.graphics.Typeface
import android.support.annotation.DrawableRes
import android.support.annotation.StyleRes
import android.support.annotation.UiThread
import android.support.design.widget.AppBarLayout
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.View
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.ui.R
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.view_app_bar_layout.view.*

/**
 * Description: Wraps the layout and common functionality for the main collapsing [AppBarLayout] in
 * this app.
 */
class ArticMainAppBarLayout(context: Context, attrs: AttributeSet? = null) : AppBarLayout(context, attrs) {
    private val disposeBag: DisposeBag = DisposeBag()
    @StyleRes
    private val expandedDefaultTextAppearance: Int
    @StyleRes
    private val expandedFixedSizeTextAppearance: Int

    private var clickConsumer: Consumer<Unit>? = null
    private var expandedTypeface: Typeface? = null

    init {
        View.inflate(context, R.layout.view_app_bar_layout, this)
        fitsSystemWindows = true

        var expandedDefaultAppearance: Int = R.style.PageTitleLargeWhite
        var expandedFixedSizeAppearance: Int = R.style.PageTitleLargeWhite_FixedSize

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.ArticMainAppBarLayout,
                    0, 0
            )
            setIcon(a.getResourceId(R.styleable.ArticMainAppBarLayout_icon, 0))
            setBackgroundImage(a.getResourceId(R.styleable.ArticMainAppBarLayout_backgroundImage, 0))
            setBackgroundImagePadding(a.getDimension(R.styleable.ArticMainAppBarLayout_backgroundImagePadding, 0f))
            subTitle.text = a.getString(R.styleable.ArticMainAppBarLayout_subtitle)

            expandedDefaultAppearance = a.getResourceId(
                    R.styleable.ArticMainAppBarLayout_expandedTitleStyle,
                    expandedDefaultAppearance
            )
            expandedFixedSizeAppearance = a.getResourceId(
                    R.styleable.ArticMainAppBarLayout_expandedFixedSizeTitleStyle,
                    expandedFixedSizeAppearance
            )

            collapsingToolbar.setExpandedTitleTextAppearance(expandedDefaultAppearance)
            subTitle.text =  a.getString(R.styleable.ArticMainAppBarLayout_subtitle)
        }

        expandedTypeface = ResourcesCompat.getFont(context, R.font.ideal_sans_medium)
        expandedDefaultTextAppearance = expandedDefaultAppearance
        expandedFixedSizeTextAppearance = expandedFixedSizeAppearance

        // update our content when offset changes.
        addOnOffsetChangedListener(OnOffsetChangedListener { aBarLayout, verticalOffset ->
            val progress: Double = 1 - Math.abs(verticalOffset) / aBarLayout.totalScrollRange.toDouble()
            val progressOutOf255: Int = (progress * 255).toInt()

            // The search icon's background is also known as the 'wash'. Its state
            // must be distinct from that of its counterparts in other
            // ArticMainAppBarLayouts - hence the `Drawable::mutate` call.
            searchIcon.background.mutate().alpha = progressOutOf255
            icon.drawable.alpha = progressOutOf255
            expandedImage.drawable.alpha = progressOutOf255
            subTitle.alpha = progress.toFloat()
        })

        postDelayed( {
            collapsingToolbar.expandedTitleMarginBottom = container.getChildAt(2).height
        }, 50)

    }

    fun setIcon(@DrawableRes iconId: Int) {
        icon.setImageResource(iconId)
    }

    fun setSubtitleText(text: String?) {
        subTitle.text = text
    }

    fun setBackgroundImage(@DrawableRes imageId: Int) {
        expandedImage.setImageResource(imageId)
    }

    fun setBackgroundImagePadding(padding: Float) {
        expandedImage.setPadding(0, padding.toInt(), 0,0)
    }

    fun setOnSearchClickedConsumer(clickConsumer: Consumer<Unit>) {
        this.clickConsumer = clickConsumer
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        searchIcon.clicks()
                .defaultThrottle()
                .subscribe {
                    clickConsumer?.accept(it)
                }
                .disposedBy(disposeBag)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposeBag.clear()
    }

    @UiThread
    fun adaptExpandedTextAppearance() {
        collapsingToolbar.run {
            setExpandedTitleTypeface(expandedTypeface)
        }
    }

}