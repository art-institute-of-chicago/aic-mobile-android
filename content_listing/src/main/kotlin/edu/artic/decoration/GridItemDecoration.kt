package edu.artic.decoration

import android.content.res.Resources
import android.graphics.Rect
import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Simple ItemDecoration that keeps track of useful [Dimensions].
 *
 * This makes it straightforward to load the best values in the current
 * [Resources] - just
 * 1. implement [createDimensions]
 * 2. call `super.getItemOffsets(outRect, view, parent, state)` at the start of
 * your override of [getItemOffsets]
 * 3. use [dimensions] in the rest of [getItemOffsets] as needed
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
abstract class GridItemDecoration(protected open val spanCount: Int = 1) : RecyclerView.ItemDecoration() {

    /**
     * Use this to simplify implementations of [getItemOffsets] and [onDrawOver] (where applicable).
     *
     * Implementors are free to interpret these values as they see fit, but we've
     * attached some recommended usage in the below docs.
     */
    protected interface Dimensions {
        /**
         * Preferred vertical offset, in pixels.
         */
        val vertical : Int
        /**
         * Preferred horizontal offset, in pixels.
         */
        val horizontal : Int
        /**
         * Preferred vertical offset for the first item or row of items in the [RecyclerView].
         */
        val topMostVertical : Int

        /**
         * Convenience function for subclasses which wish to use [vertical] for
         * both top and bottom margins.
         */
        fun halfOfVertical() : Int = (vertical / 2.0f).toInt()
    }

    /**
     * Access the singleton created in [createDimensions].
     */
    protected lateinit var dimensions: Dimensions

    protected abstract fun createDimensions(res: Resources) : Dimensions

    /**
     * Represents information about whether [dimensions] has a value yet.
     */
    private fun needsDimensions(): Boolean {
        return !this::dimensions.isInitialized
    }

    /**
     * Initialize [dimensions] by calling [createDimensions], if appropriate.
     *
     * See [here][RecyclerView.ItemDecoration.getItemOffsets] for more details about
     * what to add to overrides of this function.
     */
    @CallSuper
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (needsDimensions()) {
            dimensions = createDimensions(view.resources)
        }
    }
}