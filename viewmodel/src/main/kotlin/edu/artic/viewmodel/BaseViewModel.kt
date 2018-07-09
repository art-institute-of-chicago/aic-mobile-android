package edu.artic.viewmodel

import android.arch.lifecycle.ViewModel
import com.fuzz.rx.DisposeBag

open class BaseViewModel : ViewModel() {

    protected val disposeBag = DisposeBag()

    open fun cleanup() {
        disposeBag.clear()
    }
}