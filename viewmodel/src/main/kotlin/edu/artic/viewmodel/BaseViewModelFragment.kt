package edu.artic.viewmodel

import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.View
import edu.artic.ui.BaseFragment
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * Description: Provides common [ParentViewModel] methods for subclasses, so duplicate code doesn't
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
     * If true we resolve our [ViewModel] instances via our [ViewModelFactory] class.
     */
    open val useFactory: Boolean
        get() = true

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val viewModelLazy: InvalidatableLazyImpl<TViewModel> = invalidatableLazy {
        val viewModelProvider = if (useFragmentForProvider()) {
            getViewModelProvider(viewModelFactory)
        } else {
            baseActivity.viewModelProvider(useFactory, viewModelFactory)
        }
        return@invalidatableLazy getViewModelForClass(viewModelProvider, viewModelClass)
    }
    val viewModel: TViewModel by viewModelLazy

    val viewModelExists: Boolean
        get() = viewModelLazy.isInitialized()

    protected open fun getViewModelForClass(viewModelProvider: ViewModelProvider,
                                            kClass: KClass<TViewModel>): TViewModel =
            viewModelProvider.get(kClass.java)

    protected open fun onViewModelCreated(viewModel : TViewModel) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!viewModelExists) {
            val viewModel = viewModel
            onViewModelCreated(viewModel)
        }
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

}