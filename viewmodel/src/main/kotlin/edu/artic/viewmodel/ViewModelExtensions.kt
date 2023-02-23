package edu.artic.viewmodel

import androidx.lifecycle.ViewModelProvider

/**
 * Description:
 */
fun androidx.fragment.app.FragmentActivity.viewModelProvider(
    useFactory: Boolean,
    viewModelFactory: ViewModelFactory,
): ViewModelProvider = when {
    useFactory -> ViewModelProvider(this, viewModelFactory)
    else -> ViewModelProvider(this)
}

fun BaseViewModelFragment<*, *>.getViewModelProvider(viewModelFactory: ViewModelFactory): ViewModelProvider =
    when {
        this.useFactory -> ViewModelProvider(this, viewModelFactory)
        else -> ViewModelProvider(this)
    }