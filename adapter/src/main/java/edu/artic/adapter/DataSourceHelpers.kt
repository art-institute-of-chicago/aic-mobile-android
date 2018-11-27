package edu.artic.adapter

import android.arch.paging.DataSource

// TODO: add javadoc
inline fun <Key, Value> dataSourceFactory(crossinline createFn: () -> DataSource<Key, Value>) = object : DataSource.Factory<Key, Value>() {
    override fun create(): DataSource<Key, Value> = createFn()
}