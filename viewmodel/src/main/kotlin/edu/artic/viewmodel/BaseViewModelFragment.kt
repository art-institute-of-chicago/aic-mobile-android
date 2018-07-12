package edu.artic.viewmodel

import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.View
import edu.artic.ui.BaseActivity
import edu.artic.ui.BaseFragment
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class BaseViewModelFragment<TViewModel : BaseViewModel> : BaseFragment() {

    protected abstract val viewModelClass: KClass<TViewModel>


    /**
     * @return True by default if we use fragment for view model provider. Otherwise we use the [BaseActivity]
     * * as where the [TViewModel] lives.
     */
    protected open fun useFragmentForProvider(): Boolean = true

    /**
     * If true we resolve our [ViewModel] instances via our [ViewModelFactory] class.
     */
    open val useFactory: Boolean
        get() = true


    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val viewModelExists: Boolean
        get() = viewModelLazy.isInitialized()


    private val viewModelLazy: InvalidatableLazyImpl<TViewModel> = invalidatableLazy {
        val viewModelProvider = if (useFragmentForProvider()) {
            getViewModelProvider(viewModelFactory)
        } else {
            baseActivity.viewModelProvider(useFactory, viewModelFactory)
        }
        return@invalidatableLazy getViewModelForClass(viewModelProvider, viewModelClass)
    }

    val viewModel: TViewModel by viewModelLazy

    protected open fun getViewModelForClass(viewModelProvider: ViewModelProvider,
                                            kClass: KClass<TViewModel>): TViewModel =
            viewModelProvider.get(kClass.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!viewModelExists) {
            val viewModel = viewModel
            viewModel.onClearedListener = this::onCleared
            viewModel.register(this)
            onRegisterViewModel(viewModel)
        }
    }

    /**
     * Called when viewmodel is first initialized. Call initial setup methods on the [TViewModel] here.
     */
    protected open fun onRegisterViewModel(viewModel: TViewModel) = Unit

    override fun onDestroyView() {
        // attempt cleanup. if activity destroyed we will ignore this call here.
        try {
            if (viewModelExists) {
                viewModel.cleanup()
            }
        } catch (i: IllegalStateException) {

        }
        super.onDestroyView()
    }

    private fun onCleared() {
        if (!useFragmentForProvider()) {
            viewModel.cleanup() // cleanup connections here.
        }
        viewModelLazy.invalidate()
    }
}