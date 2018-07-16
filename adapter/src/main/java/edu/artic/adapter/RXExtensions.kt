package edu.artic.adapter

import android.arch.paging.PagedList
import android.support.v7.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.util.concurrent.TimeUnit

/**
 * Description:
 */
fun <TModel : Any> BaseRecyclerViewAdapter<TModel, *>.itemChanges() = Consumer<List<TModel>> { setItemsList(it) }

fun <TModel : Any> BaseRecyclerViewAdapter<TModel, *>.pagedListChanges() = Consumer<PagedList<TModel>> { setPagedList(it) }

/**
 * Scrolls to bottom of [RecyclerView] after animation completes to ensure its visible.
 */
fun RecyclerView.delayScrollToBottom(): Disposable =
        Observable.just(Unit)
                .delay(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    smoothScrollToPosition(0)
                }