package edu.artic.viewmodel

import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.View
import edu.artic.ui.BaseActivity
import edu.artic.ui.BaseFragment
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * Description: Provides common ParentViewModel methods for subclasses, so duplicate code doesn't
 * happen.
 */
abstract class BaseViewModelFragment<TViewModel : BaseViewModel> : BaseFragment() {

    protected abstract val viewModelClass: KClass<TViewModel>
    /**
     * @return True by default if we use fragment for view model provider. Otherwise we use the [BaseActivity]
     * * as where the [TViewModel] lives.
     */
    protected open fun useFragmentForProvider(): Boolean = true

    /**
     * If true we resolve our [BaseViewModel] instances via our [ViewModelFactory] class.
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
    var isViewJustCreated: Boolean = true
    protected open fun getViewModelForClass(viewModelProvider: ViewModelProvider,
                                            kClass: KClass<TViewModel>): TViewModel =
            viewModelProvider.get(kClass.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewJustCreated = true
        if (!viewModelExists) {
            val viewModel = viewModel
            viewModel.onClearedListener = this::onCleared
            viewModel.register(this)
            onRegisterViewModel(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isViewJustCreated) {
            setupBindings(viewModel)
        }
        setupNavigationBindings(viewModel)
    }


    override fun onPause() {
        super.onPause()
        navigationDisposeBag.clear()
    }


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

    /**
     * Called when viewmodel is first initialized. Call initial setup methods on the [TViewModel] here.
     */
    protected open fun onRegisterViewModel(viewModel: TViewModel) = Unit

    protected open fun setupBindings(viewModel: TViewModel) = Unit

    protected open fun setupNavigationBindings(viewModel: TViewModel) = Unit
}