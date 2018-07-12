package edu.artic.viewmodel

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.FragmentActivity

/**
 * Description:
 */
fun FragmentActivity.viewModelProvider(useFactory: Boolean, viewModelFactory: ViewModelFactory): ViewModelProvider = when {
    useFactory -> ViewModelProviders.of(this, viewModelFactory)
    else -> ViewModelProviders.of(this)
}

fun BaseViewModelFragment<*>.getViewModelProvider(viewModelFactory: ViewModelFactory): ViewModelProvider = when {
    this.useFactory -> ViewModelProviders.of(this, viewModelFactory)
    else -> ViewModelProviders.of(this)
}