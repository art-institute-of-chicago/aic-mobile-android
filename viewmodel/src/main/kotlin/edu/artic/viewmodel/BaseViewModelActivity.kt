package edu.artic.viewmodel

import edu.artic.ui.BaseActivity
import kotlin.reflect.KClass

abstract class BaseViewModelActivity<TViewModel : BaseViewModel> : BaseActivity() {

    protected abstract val viewModelClass : KClass<TViewModel>

    open val useFactory: Boolean
    get() = true
}