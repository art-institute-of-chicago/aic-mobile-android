package edu.artic.language

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.support.annotation.Px
import android.support.annotation.UiThread
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.ViewLayoutChangeEvent
import com.jakewharton.rxbinding2.view.layoutChangeEvents
import edu.artic.base.utils.dpToPixels
import edu.artic.media.ui.R
import io.reactivex.disposables.Disposable

/**
 * This class wraps a single View, to which it gives a dynamic [background][View.getBackground].
 *
 * The appearance of this background is inspired by Material Design's `Chip`
 * component. As the official library for that component was still in
 * development at the time of writing (August 2018), we simulate the relevant
 * parts here in [generateChipBackground].
 *
 * Unfortunately, it's quite easy to trigger a feedback loop between
 * [android.support.constraint.ConstraintLayout] and [View.measure]. Avoid
 * that by ensuring that [selectorView] has explicit width constraints. If
 * the size of that View can be defined at compile-time, consider defining
 * a background for the View in XML instead of using this class.
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
class LanguageSelectorViewBackground(private val selectorView: ViewGroup) {

    fun listenToLayoutChanges(): Disposable {
        return selectorView.layoutChangeEvents().subscribe(this::onChange)
    }

    private fun onChange(event: ViewLayoutChangeEvent) {
        if (event.left() != event.right()) {
            event.view().apply {
                background = generateChipBackground(this, event)
                requestLayout()
            }
        }
    }

    /**
     * Create and return a new `Chip`-style [Drawable].
     *
     * The general appearance of the generated [Drawable] is of a rounded rectangle
     * with a single 'caret' just inside its end. The positioning of that caret is
     * delegated off to the internal function [defineNeededInsets].
     */
    @UiThread
    private fun generateChipBackground(target: View, event: ViewLayoutChangeEvent): Drawable {
        val localContext: Context = target.context

        val shaped: GradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(localContext, R.color.black60alpha))
            val radiusInPixels: Float = Math.min(event.newWidth, event.newHeight) / 2f
            cornerRadius = radiusInPixels
        }

        val endCaret: Drawable? = AppCompatResources.getDrawable(localContext, R.drawable.ic_language_caret)
        if (endCaret != null) {
            val idealPadding = event.newWidth - endCaret.intrinsicWidth - localContext.resources.dpToPixels(16f).toInt()
            val layered = LayerDrawable(arrayOf(shaped, endCaret))
            defineNeededInsets(target, idealPadding, layered)

            return layered
        } else {
            return shaped
        }
    }

    /**
     * Assign the desired inset to the second layer in [bgLayers].
     *
     * NB: While there _is_ such a thing as an
     * [InsetDrawable][android.graphics.drawable.InsetDrawable], we'd go
     * through an extra 3 or so measure passes and allocate a bunch more
     * memory if we wrapped any of the layers in that. This method can get
     * away with just calling [LayerDrawable.setLayerInset] once, then
     * returning, so that's what it does.
     *
     * @param host this is the View which will receive the background
     * @param spaceForHost how much space (in pixels) [host] will need to
     * display any non-background content. May not be negative.
     * @param bgLayers the various layers which will be passed to [View.setBackground]
     * soon after this method returns
     */
    private fun defineNeededInsets(host: View, @Px spaceForHost: Int, bgLayers: LayerDrawable) {
        val foremostPadding: Rect = resolveRelativity(host, spaceForHost)
        val padding = host.resources.dpToPixels(6f).toInt()

        /**
         * Adds padding to background drawable.
         */
        bgLayers.setLayerInset(
                0,
                0,
                padding,
                0,
                padding
        )

        bgLayers.setLayerInset(
                1,
                foremostPadding.left,
                foremostPadding.top,
                foremostPadding.right,
                foremostPadding.bottom
        )
    }

    /**
     * Map the given [inset] to whatever 'end' currently means for [target].
     *
     * We use the value of [layout direction][View.getLayoutDirection], which
     * should already have been resolved. Hence, [inset] will be set to
     * * [Rect.left] if target is in [View.LAYOUT_DIRECTION_LTR]
     * and
     * * [Rect.right] if target is in [View.LAYOUT_DIRECTION_RTL]
     */
    private fun resolveRelativity(target: View, inset: Int): Rect {
        val resolvedInset = Rect()
        when (target.layoutDirection) {
            View.LAYOUT_DIRECTION_LTR -> {
                resolvedInset.left = inset
            }
            View.LAYOUT_DIRECTION_RTL -> {
                resolvedInset.right = inset
            }
        }
        val toInt = target.resources.dpToPixels(4f).toInt()
        resolvedInset.top = toInt
        resolvedInset.bottom = toInt
        return resolvedInset
    }

    val ViewLayoutChangeEvent.newWidth: Int
        get() = Math.abs(left() - right())

    val ViewLayoutChangeEvent.newHeight: Int
        get() = Math.abs(top() - bottom())
}