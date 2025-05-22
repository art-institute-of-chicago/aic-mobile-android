package edu.artic.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.annotation.UiThread
import androidx.core.content.res.ResourcesCompat
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.ui.R
import edu.artic.ui.databinding.ViewAppBarLayoutBinding
import io.reactivex.functions.Consumer

//import kotlinx.android.synthetic.main.view_app_bar_layout.view.*

/**
 * Description: Wraps the layout and common functionality for the main collapsing [AppBarLayout] in
 * this app.
 */
class ArticMainAppBarLayout(context: Context, attrs: AttributeSet? = null) :
    AppBarLayout(context, attrs) {
    private val disposeBag: DisposeBag = DisposeBag()

    @StyleRes
    private val expandedDefaultTextAppearance: Int

    @StyleRes
    private val expandedFixedSizeTextAppearance: Int

    private var clickConsumer: Consumer<Unit>? = null
    private var expandedTypeface: Typeface? = null
    private val binding: ViewAppBarLayoutBinding

    init {
        binding =
            ViewAppBarLayoutBinding.inflate(LayoutInflater.from(context), this)



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
            setBackgroundImage(
                a.getResourceId(
                    R.styleable.ArticMainAppBarLayout_backgroundImage,
                    0
                )
            )
            setBackgroundImagePadding(
                a.getDimension(
                    R.styleable.ArticMainAppBarLayout_backgroundImagePadding,
                    0f
                )
            )
            binding.subTitle.text = a.getString(R.styleable.ArticMainAppBarLayout_subtitle)

            expandedDefaultAppearance = a.getResourceId(
                R.styleable.ArticMainAppBarLayout_expandedTitleStyle,
                expandedDefaultAppearance
            )
            expandedFixedSizeAppearance = a.getResourceId(
                R.styleable.ArticMainAppBarLayout_expandedFixedSizeTitleStyle,
                expandedFixedSizeAppearance
            )

            binding.collapsingToolbar.setExpandedTitleTextAppearance(expandedDefaultAppearance)
            binding.subTitle.text = a.getString(R.styleable.ArticMainAppBarLayout_subtitle)
        }

        expandedTypeface = ResourcesCompat.getFont(context, R.font.ideal_sans_medium)
        expandedDefaultTextAppearance = expandedDefaultAppearance
        expandedFixedSizeTextAppearance = expandedFixedSizeAppearance

        // update our content when offset changes.
        addOnOffsetChangedListener(OnOffsetChangedListener { aBarLayout, verticalOffset ->
            val progress: Double =
                1 - Math.abs(verticalOffset) / aBarLayout.totalScrollRange.toDouble()
            val progressOutOf255: Int = (progress * 255).toInt()

            // The search icon's background is also known as the 'wash'. Its state
            // must be distinct from that of its counterparts in other
            // ArticMainAppBarLayouts - hence the `Drawable::mutate` call.
            binding.searchIcon.background.mutate().alpha = progressOutOf255
            binding.icon.drawable.alpha = progressOutOf255
            binding.expandedImage.drawable.alpha = progressOutOf255
            binding.subTitle.alpha = progress.toFloat()
        })

        postDelayed({
            binding.collapsingToolbar.expandedTitleMarginBottom =
                binding.container.getChildAt(2).height
        }, 50)


    }

    fun setIcon(@DrawableRes iconId: Int) {
        binding.icon.setImageResource(iconId)
    }

    fun setSubtitleText(text: String?) {
        binding.subTitle.text = text
    }

    fun setBackgroundImage(@DrawableRes imageId: Int) {
        binding.expandedImage.setImageResource(imageId)
    }

    fun setBackgroundImagePadding(padding: Float) {
        binding.expandedImage.setPadding(0, padding.toInt(), 0, 0)
    }

    fun setOnSearchClickedConsumer(clickConsumer: Consumer<Unit>) {
        this.clickConsumer = clickConsumer
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding.searchIcon.clicks()
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
        binding.collapsingToolbar.run {
            val titleLength = title?.toString().orEmpty().length

            if (titleLength < FULL_SIZE_TEXT_BREAKPOINT) {
                setExpandedTitleTextAppearance(expandedDefaultTextAppearance)
            } else {
                setExpandedTitleTextAppearance(expandedFixedSizeTextAppearance)
            }
            setCollapsedTitleTypeface(expandedTypeface)
            setExpandedTitleTypeface(expandedTypeface)
        }
    }


    companion object {

        /**
         * Max length of text visible with expanded style [expandedDefaultTextAppearance].
         *
         * If title extends beyond this, callers should switch
         * [collapsingToolbar] over to [expandedFixedSizeTextAppearance].
         */
        const val FULL_SIZE_TEXT_BREAKPOINT: Int = """Welcome, And…""".length
    }

}