package edu.artic.search

import com.fuzz.retrofit.rx.requireValue
import com.jakewharton.retrofit2.adapter.rxjava2.Result
import io.reactivex.Observable
import timber.log.Timber

/**
 * Perform a simple [map] call over the emitted [Result]s.
 *
 * - If the api call [resulted in failure][Result.isError], we map to [onErrorValue].
 * - Else, if the api call [succeeded][com.fuzz.retrofit.rx.isSuccess] we map to its
 * [internal content][Result.requireValue].
 */
fun <T> Observable<Result<T>>.mapWithDefault(onErrorValue : T) : Observable<T> {
    return map {
        if (it.isError) {
            Timber.w(it.error())
            onErrorValue
        } else {
            it.requireValue()
        }
    }
}