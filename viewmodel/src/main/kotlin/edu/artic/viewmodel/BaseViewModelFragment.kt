package edu.artic.viewmodel

import edu.artic.ui.BaseFragment

abstract class BaseViewModelFragment<TViewModel : BaseViewModel> : BaseFragment() {
    /**
    * If true we resolve our [ViewModel] instances via our [ViewModelFactory] class.
    */
    open val useFactory: Boolean
        get() = true
}