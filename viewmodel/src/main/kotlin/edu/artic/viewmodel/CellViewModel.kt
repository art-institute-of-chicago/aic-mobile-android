package edu.artic.viewmodel

/**
 * Specialized [BaseViewModel] designed for use with the ':adapter` module's
 * `edu.artic.adapter.AutoHolderRecyclerViewAdapter`.
 *
 * Not intended for use with injection factories.
 *
 * These need to have a longer lifecycle than [viewDisposeBag] but shorter than
 * [BaseViewModelFragment's][BaseViewModelFragment.disposeBag] or
 * [BaseViewModelActivity's][BaseViewModelActivity.disposeBag].
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
abstract class CellViewModel() : BaseViewModel() {

    init {
    }
}