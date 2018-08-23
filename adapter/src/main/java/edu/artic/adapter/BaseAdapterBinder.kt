package edu.artic.adapter

import android.support.v7.widget.RecyclerView
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.BaseAdapter

/**
 * Description:
 */
fun <TModel> BaseRecyclerViewAdapter<TModel, *>.toBaseAdapter() = BaseAdapterBinder(this)

/**
 * Description: Provides a very simple compatibility layer between [BaseRecyclerViewAdapter]
 * and [android.widget.BaseAdapter]. It enables reusing the [BaseRecyclerViewAdapter]
 * without needing to use a different implementation.
 * @author Andrew Grosner (Fuzz)
 */
open class BaseAdapterBinder<TModel>(
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
            viewHolder = adapter.createViewHolder(viewGroup, viewType)

            if (viewHolder != null) {
                localView = viewHolder.itemView
                localView.setTag(R.id.tag_holder, viewHolder)
                localView.setTag(R.id.tag_item_type, viewType)
            }
        } else {
            viewHolder = localView.getTag(R.id.tag_holder) as BaseViewHolder?
        }

        viewHolder?.let { adapter.bindViewHolder(viewHolder, i) }
        return localView
    }

    private inner class RecyclerViewToBaseAdapterObserver : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onChanged() = notifyDataSetChanged()

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = notifyDataSetChanged()
    }
}