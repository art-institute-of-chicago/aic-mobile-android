package edu.artic.image

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.Observable


class ImageFailedToLoadError(url: String) : RuntimeException("Image Resource $url Failed to Load.")

/**
 * Description: Converts a [GlideRequest] into an [Observable] that does not complete.
 */
fun <T> RequestBuilder<T>.asRequestObservable(context: Context): Observable<T> {
    return Observable.create { emitter ->
        val target = object : SimpleTarget<T>() {
            override fun onResourceReady(resource: T, transition: Transition<in T>?) {
                emitter.onNext(resource)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
            }
        }
        into(target)
        emitter.setCancellable { Glide.with(context).clear(target) }
    }
}