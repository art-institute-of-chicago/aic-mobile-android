package edu.artic.viewmodel

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import com.fuzz.rx.DisposeBag

open class BaseViewModel : ViewModel() {

    protected val disposeBag = DisposeBag()

    /**
     * Used to register binding on views that live in view lifecycle.
     */
    val viewDisposeBag = DisposeBag()

    lateinit var lifeCycleOwner: LifecycleOwner

    var onClearedListener: (() -> Unit)? = null

    /**
     * Register bindings that outlive view cycle here.
     */
    @CallSuper
    open fun register(lifeCycleOwner: LifecycleOwner) {
        this.lifeCycleOwner = lifeCycleOwner
    }

    @CallSuper
    public override fun onCleared() {
        super.onCleared()
        disposeBag.clear()
        onClearedListener?.invoke()
    }

    /**
     * Called when view is expected to be destroyed. this is not always the case. Also
     * can be called when [onCleared] is invoked in a [BaseViewModelFragment]
     */
    open fun cleanup() {
        viewDisposeBag.clear()
    }
}