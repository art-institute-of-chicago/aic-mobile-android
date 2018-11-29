package edu.artic.adapter

import android.arch.paging.PositionalDataSource

/**
 * Description: Represents a list that only returns sublists of itself.
 */
class ListDataSource<T>(list: List<T>) : PositionalDataSource<T>() {
    private val list: List<T>

    init {
        this.list = ArrayList(list)
    }

    override fun loadInitial(params: PositionalDataSource.LoadInitialParams,
                             callback: PositionalDataSource.LoadInitialCallback<T>) {
        val totalCount = list.size

        val position = PositionalDataSource.computeInitialLoadPosition(params, totalCount)
        val loadSize = PositionalDataSource.computeInitialLoadSize(params, position, totalCount)

        // for simplicity, we could return everything immediately,
        // but we tile here since it's expected behavior
        val sublist = list.subList(position, position + loadSize)
        callback.onResult(sublist, position, totalCount)
    }

    override fun loadRange(params: PositionalDataSource.LoadRangeParams,
                           callback: PositionalDataSource.LoadRangeCallback<T>) {
        var toIndex = params.startPosition + params.loadSize
        if (toIndex > list.size) {
            toIndex = list.lastIndex
        }
        if (params.startPosition > toIndex) {
            callback.onResult(listOf()) //  return empty list to signal end.
        } else {
            callback.onResult(list.subList(params.startPosition, toIndex))
        }
    }
}