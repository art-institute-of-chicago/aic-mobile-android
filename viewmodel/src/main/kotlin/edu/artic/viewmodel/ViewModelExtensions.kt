package edu.artic.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

/**
 * Description:
 */
fun androidx.fragment.app.FragmentActivity.viewModelProvider(
    useFactory: Boolean,
    viewModelFactory: ViewModelFactory
): ViewModelProvider = when {
    useFactory -> ViewModelProviders.of(this, viewModelFactory)
    else -> ViewModelProviders.of(this)
}

fun BaseViewModelFragment<*, *>.getViewModelProvider(viewModelFactory: ViewModelFactory): ViewModelProvider =
    when {
        this.useFactory -> ViewModelProviders.of(this, viewModelFactory)
        else -> ViewModelProviders.of(this)
    }