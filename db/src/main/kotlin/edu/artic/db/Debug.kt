@file:Suppress("NOTHING_TO_INLINE")

package edu.artic.db

import android.annotation.SuppressLint
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

/**
 * Inlines the body when [BuildConfig.DEBUG] only. Will allow arbirary execution inside the object
 */
inline fun <T> T.debugTransform(fn: T.() -> T): T {
    return if (BuildConfig.DEBUG) {
        return fn()
    } else {
        this
    }
}

/**
 * Inlines the body when [BuildConfig.DEBUG] only. On release builds this will get stripped.
 */
inline fun debugApply(fn: () -> Unit) {
    if (BuildConfig.DEBUG) {
        fn()
    }
}

/**
 * An efficient debug mechanism that gets inlined,
 * this logs all events on an [Observable] in [BuildConfig.DEBUG] mode only.
 * In release mode, this is stripped out.
 */
@SuppressLint("CheckResult")
inline fun <T> Observable<T>.debug(tag: String, emitValue: Boolean = true): Observable<T> =
        debugTransform {
            doOnNext { Timber.d("~~~~$tag~~~~: NEXT ${if (emitValue) it else null}") }
                    .doOnComplete { Timber.d("~~~~$tag~~~: COMPLETED") }
                    .doOnError { Timber.e("~~~~$tag~~~~: ERROR") }
                    .doOnSubscribe { Timber.i("~~~~$tag~~~: SUBSCRIBED") }
                    .doOnDispose { Timber.i("~~~~$tag~~~~: DISPOSED") }
        }

/**
 * An efficient debug mechanism that gets inlined,
 * this logs all events on a [Flowable] in [BuildConfig.DEBUG] mode only.
 * In release mode, this is stripped out.
 */
@SuppressLint("CheckResult")
inline fun <T> Flowable<T>.debug(tag: String, emitValue: Boolean = true): Flowable<T> =
        debugTransform {
            doOnNext { Timber.d("~~~~$tag~~~~: NEXT ${if (emitValue) it else null}") }
                    .doOnCancel { Timber.i("~~~~$tag~~~: CANCELLED") }
                    .doOnComplete { Timber.d("~~~~$tag~~~: COMPLETED") }
                    .doOnError { Timber.e("~~~~$tag~~~~: ERROR") }
                    .doOnSubscribe { Timber.i("~~~~$tag~~~: SUBSCRIBED") }
        }

/**
 * An efficient debug mechanism that gets inlined,
 * this logs all events on an [Observable] in [BuildConfig.DEBUG] mode only.
 * In release mode, this is stripped out.
 */
@SuppressLint("CheckResult")
inline fun <T> Single<T>.debug(tag: String, emitValue: Boolean = true): Single<T> =
        debugTransform {
            doOnSuccess { Timber.d("~~~~$tag~~~~: SUCCESS ${if (emitValue) it else null}") }
                    .doOnError { Timber.e("~~~~$tag~~~~: ERROR") }
                    .doOnDispose { Timber.i("~~~~$tag~~~~: DISPOSED") }
                    .doOnSubscribe { Timber.i("~~~~$tag~~~: SUBSCRIBED") }
        }
