package edu.artic.adapter

import android.support.v7.widget.RecyclerView
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.BaseAdapter
import android.widget.Spinner
import android.widget.SpinnerAdapter

/**
 * Description: Convenience conversion method that turns this object into a [BaseAdapter].
 */
fun <TModel> BaseRecyclerViewAdapter<TModel, *>.toBaseAdapter(): BaseAdapter =
        BaseAdapterBinder(this)

/**
 * Retrieve original [BaseRecyclerViewAdapter] from this [BaseAdapter]. It must be created with [toBaseAdapter] first.
 * Must be a function due to Kotlin's type inference.
 */
@Suppress("UNCHECKED_CAST")
fun <TModel> SpinnerAdapter.baseRecyclerViewAdapter(): BaseRecyclerViewAdapter<TModel, BaseViewHolder> =
        (this as BaseAdapterBinder<TModel>).adapter as BaseRecyclerViewAdapter<TModel, BaseViewHolder>

/**
 * Implement this interface to add the ability for dropdown view functionality within a [Spinner], for example.
 */
interface DropDownAdapter<TModel, VH : BaseViewHolder> {

    /**
     * Return the item view based on type. The headers + footers will not be called from here. Those
     * cannot be configured to display differently in this adapter.
     */
    fun onCreateDropdownItemViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder? =
            BaseViewHolder(parent, viewType).apply {
                itemView.onDropdownHolderCreated(parent, viewType)
            }

    fun onBindDropdownItemViewHolder(baseViewHolder: VH, item: TModel, position: Int) =
            baseViewHolder.itemView.onBindDropdownView(item, position)

    fun View.onBindDropdownView(item: TModel, position: Int) = Unit

    /**
     * Called when the [BaseViewHolder] is first created in the scope of it's itemView. Perform
     * initial registering of view bindings here that respond outside normal onClickListener.
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    fun View.onDropdownHolderCreated(parent: ViewGroup, viewType: Int) = Unit
}

/**
 * Description: Provides a very simple compatibility layer between [BaseRecyclerViewAdapter]
 * and [android.widget.BaseAdapter]. It enables reusing the [BaseRecyclerViewAdapter]
 * without needing to use a different implementation.
 * @author Andrew Grosner (Fuzz)
 */
class BaseAdapterBinder<TModel>(
        val adapter: BaseRecyclerViewAdapter<TModel, *>) : BaseAdapter() {

    private var currentMaxViewType = 0
    private val cachedLayoutIdToViewTypes = SparseIntArray()

    init {
        adapter.registerAdapterDataObserver(RecyclerViewToBaseAdapterObserver())
    }

    override fun getCount() = adapter.itemCount

    override fun getItem(i: Int) = adapter.getItemOrNull(i)

    override fun getItemId(i: Int) = i.toLong()

    override fun getViewTypeCount() = 1 // override for proper view type counting

    override fun getItemViewType(position: Int): Int {
        val itemViewType = adapter.getItemViewType(position)
        var viewType = cachedLayoutIdToViewTypes.get(itemViewType, Adapter.IGNORE_ITEM_VIEW_TYPE)
        // cache and increment in order to keep ids in order as required by BaseAdapter
        if (viewType == Adapter.IGNORE_ITEM_VIEW_TYPE) {
            cachedLayoutIdToViewTypes.put(itemViewType, currentMaxViewType)
            viewType = currentMaxViewType
            currentMaxViewType++
        }
        return viewType
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View? {
        var localView = view
        val viewHolder: BaseViewHolder?
        val viewType = adapter.getItemViewType(i)
        if (localView == null || localView.getTag(R.id.tag_item_type) != viewType) {
            viewHolder = adapter.createViewHolder(viewGroup, viewType)?.also {
                localView = applyViewHolderTags(it, viewType)
            }
        } else {
            viewHolder = localView?.getTag(R.id.tag_holder) as BaseViewHolder?
        }

        viewHolder?.let { adapter.bindViewHolder(viewHolder, i) }
        return localView
    }

    /**
     * Defaults to the [getView] implementation if no helper is used.
     */
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        if (adapter is DropDownAdapter<*, *> &&
                // headers and footers are not supported in dropdowns.
                !adapter.isFooterPosition(position) &&
                !adapter.isHeaderPosition(position)) {
            @Suppress("UNCHECKED_CAST")
            val dropDownAdapter = adapter as DropDownAdapter<TModel, BaseViewHolder>
            var viewToReturn = convertView

            val viewType = adapter.getItemViewType(position)
            val viewHolder: BaseViewHolder?
            if (viewToReturn == null || viewToReturn.getTag(R.id.tag_item_type) != viewType) {
                viewHolder = adapter.onCreateDropdownItemViewHolder(parent, viewType)?.also {
                    viewToReturn = applyViewHolderTags(it, viewType)
                }
            } else {
                viewHolder = viewToReturn?.getTag(R.id.tag_holder) as BaseViewHolder?
            }

            viewHolder?.let { dropDownAdapter.onBindDropdownItemViewHolder(viewHolder, adapter.getItem(position), position) }
            return viewToReturn
        } else {
            return getView(position, convertView, parent)
        }
    }

    private fun applyViewHolderTags(viewHolder: BaseViewHolder, viewType: Int) =
            viewHolder.itemView.apply {
                setTag(R.id.tag_holder, viewHolder)
                setTag(R.id.tag_item_type, viewType)
            }

    private inner class RecyclerViewToBaseAdapterObserver : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onChanged() = notifyDataSetChanged()

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = notifyDataSetChanged()
    }
}