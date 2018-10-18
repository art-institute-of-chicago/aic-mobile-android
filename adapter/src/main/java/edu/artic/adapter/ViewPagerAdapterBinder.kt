package edu.artic.adapter

import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup

/**
 * @author Sameer Dhakal (Fuzz)
 */

fun <TModel> BaseRecyclerViewAdapter<TModel, *>.toPagerAdapter() = ViewPagerAdapterBinder(this)

/**
 * Description: Binds a [BaseRecyclerViewAdapter] to a [PagerAdapter]
 */
open class ViewPagerAdapterBinder<TModel>(
        val adapter: BaseRecyclerViewAdapter<TModel, *>) : PagerAdapter() {

    init {
        adapter.registerAdapterDataObserver(RecyclerViewToBaseAdapterObserver())
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

    override fun getCount(): Int = adapter.itemsListCount

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val viewHolder = adapter.createViewHolder(container, adapter.getItemViewType(position))
        adapter.bindViewHolder(viewHolder, position)
        container.addView(viewHolder.itemView)
        return viewHolder.itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView((`object` as View))
    }

    private inner class RecyclerViewToBaseAdapterObserver : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onChanged() = notifyDataSetChanged()

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = notifyDataSetChanged()

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = notifyDataSetChanged()
    }
}