package edu.artic.adapter

import android.support.v7.util.DiffUtil
import android.view.View
import android.view.ViewGroup

/**
 * Description: Assumes the ViewHolder is a [BaseViewHolder] and then scopes the [onBindView] method
 * to the itemView of the [BaseViewHolder] to provide simplified kotlin-extensions access.
 *
 * Subclasses are expected to make a very clear distinction between their
 * layouts and their binding logic:
 *
 * * In [getLayoutResId], check the current item at that position and return
 *    the id of the layout needed for binding it.
 * * In [onBindView], implement binding logic in reference to `this` (where
 *    `this` is the inflated version of that same layout).
 */
abstract class AutoHolderRecyclerViewAdapter<TModel> : BaseRecyclerViewAdapter<TModel, BaseViewHolder> {

    constructor(diffItemCallback: DiffUtil.ItemCallback<TModel>) : super(diffItemCallback)

    constructor() : super()

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder =
            BaseViewHolder(parent, viewType).apply {
                itemView.onHolderCreated(parent, viewType)
            }

    override fun onBindViewHolder(holder: BaseViewHolder, item: TModel?, position: Int) {
        holder.itemView.onBindNullableView(item, position)
    }

    /**
     * Called when an item is ready to be bound. It may be null.
     *
     * TODO: Point out that this is defined as a View extension method
     */
    private fun View.onBindNullableView(item: TModel?, position: Int) {
        if (item != null) {
            onBindView(item, position)
        } else {
            onBindPlaceHolder(position)
        }
    }

    /**
     * Called when [TModel] is null. only useful for paginated list contents.
     *
     * TODO: Maybe delete?
     */
    protected open fun View.onBindPlaceHolder(position: Int) = Unit

    /**
     * Called when [TModel] is not null and ready for binding.
     */
    abstract fun View.onBindView(item: TModel, position: Int)

    /**
     * Called when the [BaseViewHolder] is first created in the scope of it's itemView. Perform
     * initial registering of view bindings here that respond outside normal onClickListener.
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    protected fun View.onHolderCreated(parent: ViewGroup, viewType: Int) = Unit
}