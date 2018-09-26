package edu.artic.util

import com.fuzz.retrofit.rx.requireValue
import com.jakewharton.retrofit2.adapter.rxjava2.Result
import edu.artic.base.NetworkException
import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

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


/**
 * Delay this observable until the point when
 *
 * 1. [other] has emitted at least one value since this method call, and
 * 2. [other] is not currently emitting, and
 * 3. the last emission from [other] was at least 1 second in the past
 *
 * *Be careful*: if [other] does not ever emit, the observable will
 * wait forever. Consider registering an [Observable.timeout] on
 * the returned object to address that concern.
 */
fun <T> Observable<T>.waitForASecondOfCalmIn(other: Subject<*>): Observable<T> {
    return zipWith(other.debounce(1, TimeUnit.SECONDS))
            .map { (original, _) -> original }
}

/**
 * Extension method for handling network errors.
 *
 * TODO: localize the error message
 */
fun <T> Observable<T>.handleNetworkError(): Observable<T> {
    return this.onErrorResumeNext { t: Throwable ->
        var exception = t
        if (t is UnknownHostException) {
            exception = NetworkException("No internet connection.")
        } else if (t is SocketTimeoutException) {
            exception = NetworkException("Network time out. Please try again.")
        }
        Observable.error(exception)
    }
}
