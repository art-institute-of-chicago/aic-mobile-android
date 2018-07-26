package edu.artic.base.utils

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.support.v4.app.Fragment
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * Adds listener that only cares about returning the drawable on success, and notifying that there
 * was a failure. nothing else.
 */
inline fun RequestBuilder<Drawable>.listenerClean(
        crossinline onFailed: () -> Boolean,
        crossinline onResourceReady: (resource: Drawable) -> Boolean)
        : RequestBuilder<Drawable> {
    return this.listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
            return onFailed.invoke()
        }

        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
            return onResourceReady(resource)
        }

    })
}

fun RequestBuilder<Drawable>.listenerAnimateSharedTransaction(fragment: Fragment, image: ImageView): RequestBuilder<Drawable> {
    return listenerClean({
        fragment.startPostponedEnterTransition()
        return@listenerClean false

    }, { resource: Drawable ->

        // Adds a nice animator to scale the container down to proper aspect ratio
        val parentWidth = (image.parent as View).width
        val newHeight = if (resource.intrinsicWidth > resource.intrinsicHeight) {
            ((resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()) * parentWidth).toInt()
        } else {
            parentWidth
        }
        if (newHeight != parentWidth) {
            val animator = ValueAnimator.ofInt(parentWidth, newHeight)
            animator.addUpdateListener { valAnimator ->
                val params = image.layoutParams
                params.apply {
                    width = parentWidth
                    height = valAnimator.animatedValue as Int
                }
                image.layoutParams = params
            }
            animator.duration = 750
            animator.start()
        }

        fragment.startPostponedEnterTransition()
        return@listenerClean false
    })
}

fun RequestBuilder<Drawable>.listenerSetHeight(image: ImageView): RequestBuilder<Drawable> {
    return listenerClean({
        return@listenerClean false

    }, { resource: Drawable ->

        // Adds a nice animator to scale the container down to proper aspect ratio
        val parentWidth = (image.parent as View).width
        val newHeight = if (resource.intrinsicWidth > resource.intrinsicHeight) {
            ((resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()) * parentWidth).toInt()
        } else {
            parentWidth
        }
        val params = image.layoutParams
        params.apply {
            width = parentWidth
            height = newHeight
        }
        image.layoutParams = params
        return@listenerClean false
    })
}