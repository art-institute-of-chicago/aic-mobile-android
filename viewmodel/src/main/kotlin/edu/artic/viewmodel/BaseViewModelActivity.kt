package edu.artic.viewmodel

import android.os.Bundle
import edu.artic.ui.BaseActivity
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class BaseViewModelActivity<TViewModel : BaseViewModel> : BaseActivity() {

    protected abstract val viewModelClass: KClass<TViewModel>

    open val useFactory: Boolean
        get() = true

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val viewModelLazy: InvalidatableLazyImpl<TViewModel> = invalidatableLazy {
        viewModelProvider(useFactory, viewModelFactory).get(viewModelClass.java)
    }
    val viewModel: TViewModel by viewModelLazy

    val viewModelExists: Boolean
        get() = viewModelLazy.isInitialized()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!viewModelExists) {
            val viewModel = viewModel
            onViewModelCreated(viewModel)
        }

    }

    /**
     * Called when [TViewModel] has been created.
     */
    protected open fun onViewModelCreated(viewModel: TViewModel) = Unit

    override fun onDestroy() {
        try {
            if(viewModelExists) {
                viewModel.cleanup()
            }
        } catch (i : IllegalStateException) {

        }
        super.onDestroy()
    }
}