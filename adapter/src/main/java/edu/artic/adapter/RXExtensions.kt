package edu.artic.adapter

import android.arch.paging.PagedList
import android.support.v7.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.util.concurrent.TimeUnit

/**
 * This lets you submit new changes to the adapter.
 *
 * Call [com.fuzz.rx.bindToMain] with this as a parameter
 * to send new lists of items (i.e. backing data) to the
 * adapter.
 */
fun <TModel : Any> BaseRecyclerViewAdapter<TModel, *>.itemChanges() = Consumer<List<TModel>> { setItemsList(it) }

fun <TModel : Any> BaseRecyclerViewAdapter<TModel, *>.pagedListChanges() = Consumer<PagedList<TModel>> { setPagedList(it) }

/**
 * This lets you [observe on][Observable] that gets called by
 * [certain views][BaseViewHolder.itemView] in this adapter.
 *
 * Binding is set up by the default implementation of
 * [BaseRecyclerViewAdapter.setItemClickListener], so if you
 * override that method the events might not come through.
 *
 * See also [BaseRecyclerViewAdapter.onItemClickListener] and
 * [BaseRecyclerViewAdapter.onItemClicked].
 */
fun <TModel : Any> BaseRecyclerViewAdapter<TModel, *>.itemClicks(): Observable<TModel> =
        Observable.create { emitter ->
            onItemClickListener = edu.artic.adapter.onItemClickListener {
                emitter.onNext(it)
            }
        }

/**
 * Just like [itemClicks] - see doc on that function for more info.
 *
 * Please reference [BaseRecyclerViewAdapter.onItemPositionClicked] for
 * details on the Int value used for 'position' here.
 */
fun <TModel : Any> BaseRecyclerViewAdapter<TModel, *>.itemClicksWithPosition(): Observable<Pair<Int,TModel>> =
        Observable.create { emitter ->
            onItemClickListener = edu.artic.adapter.onItemClickListenerWithPosition { pos, model ->
                emitter.onNext(pos to model)
            }
        }
