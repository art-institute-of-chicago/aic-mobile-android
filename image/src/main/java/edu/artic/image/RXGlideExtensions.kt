package edu.artic.image

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers


/**
 * Description: Converts a [GlideRequest] into an [Observable] that does not complete.
 */
fun <T> RequestBuilder<T>.asRequestObservable(context: Context): Observable<T> {
    return Observable.create { emitter ->
        val target = object : SimpleTarget<T>() {
            override fun onResourceReady(resource: T, transition: Transition<in T>?) {
                emitter.onNext(resource)
                emitter.onComplete()
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                emitter.onComplete()
            }
        }
        into(target)
        emitter.setCancellable {
            // Glide checks if activity is destroyed and then crashes if it is. We don't call cancel
            // in that case.
            if (context !is Activity || !context.isDestroyed) {
                // post on main thread.
                AndroidSchedulers.mainThread().scheduleDirect { Glide.with(context).clear(target) }
            }
        }
    }
}