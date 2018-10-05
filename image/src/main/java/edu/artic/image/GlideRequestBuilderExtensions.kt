package edu.artic.image

import android.animation.ValueAnimator
import android.graphics.Bitmap
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
 * Simple mechanism for loading a full image and a thumbnail image from separate urls.
 *
 * The thumbnail request will only take the options currently set on 'this'; future
 * calls to e.g. [this.setListener()][RequestBuilder.listener] will not affect it.
 *
 * Note that this method accepts null parameters since the underlying library
 * accepts them. There are few (if any) tangible benefits to passing in null.
 *
 * @see [RequestBuilder.load]
 * @see [RequestBuilder.thumbnail]
 */
fun RequestBuilder<Bitmap>.loadWithThumbnail(thumbUri: String?, fullUri: String?): RequestBuilder<Bitmap> {
    return this.thumbnail(
            clone().load(thumbUri)
    ).load(fullUri)
}

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

fun RequestBuilder<Drawable>.listenerAnimateSharedTransaction(
        fragment: Fragment,
        image: ImageView,
        scaleInfo: ImageViewScaleInfo? = null
): RequestBuilder<Drawable> {
    return listenerClean({
        scaleInfo?.let {
            image.scaleType = scaleInfo.placeHolderScaleType
        }
        fragment.startPostponedEnterTransition()
        return@listenerClean false

    }, { resource: Drawable ->
        scaleInfo?.let {
            image.scaleType = scaleInfo.imageScaleType
        }
        // Adds a nice animator to scale the container down to proper aspect ratio
        val parentWidth = (image.parent as View).width
        val newHeight = if (resource.intrinsicWidth > resource.intrinsicHeight) {
            ((resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()) * parentWidth).toInt()
        } else {
            parentWidth
        }
        if (newHeight != parentWidth && newHeight > 0) {
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

fun RequestBuilder<Drawable>.listenerSetHeight(
        image: ImageView,
        scaleInfo: ImageViewScaleInfo? = null
): RequestBuilder<Drawable> {
    return listenerClean({
        scaleInfo?.let {
            image.scaleType = scaleInfo.placeHolderScaleType
        }
        return@listenerClean false

    }, { resource: Drawable ->

        scaleInfo?.let {
            image.scaleType = scaleInfo.imageScaleType
        }

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

/**
 * This method updates ImageView.ScaleType of imageView.
 * - Uses placeHolderScaleType when imageView is displaying placeholder image.
 * - Uses imageScaleType when glide is done loading image and about to set image to imageView.
 *
 * @param imageView ImageView
 * @param placeHolderScaleType ImageView.ScaleType for placeHolder
 * @param imageScaleType ImageView.ScaleType for the imageView when request is completed successfully
 */
fun RequestBuilder<Drawable>.updateImageScaleType(imageView: ImageView,
                                                  scaleInfo: ImageViewScaleInfo
): RequestBuilder<Drawable> {
    return listenerClean({
        imageView.scaleType = scaleInfo.placeHolderScaleType
        return@listenerClean false
    }, {
        imageView.scaleType = scaleInfo.imageScaleType
        return@listenerClean false
    })
}

data class ImageViewScaleInfo(
        val placeHolderScaleType: ImageView.ScaleType,
        val imageScaleType: ImageView.ScaleType
)