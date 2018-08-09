package edu.artic.view

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator

/**
 * @author Sameer Dhakal (Fuzz)
 */
open class AnimListenerImpl : Animator.AnimatorListener {
    override fun onAnimationRepeat(animation: Animator?) {}
    override fun onAnimationEnd(animation: Animator?) {}
    override fun onAnimationCancel(animation: Animator?) {}
    override fun onAnimationStart(animation: Animator?) {}
}

inline fun <reified V> changeValue(from: V,
                                   to: V,
                                   duration: Long,
                                   crossinline update: (value: V) -> Unit
) {
    val vH: PropertyValuesHolder = when (from) {
        is Int -> PropertyValuesHolder.ofInt("prop", from as Int, to as Int)
        is Float -> PropertyValuesHolder.ofFloat("prop", from as Float, to as Float)
        else -> throw UnsupportedOperationException("$from and $to types are not supported")
    }

    ValueAnimator.ofPropertyValuesHolder(vH).apply {
        this.duration = duration
        addUpdateListener {
            update(this.getAnimatedValue("prop") as V)
        }
        start()
    }
}