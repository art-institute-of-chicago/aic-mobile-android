package edu.artic.viewmodel

import com.fuzz.rx.DisposeBag

/**
 * Specialized [BaseViewModel] designed for use with the ':adapter` module's
 * `edu.artic.adapter.AutoHolderRecyclerViewAdapter`.
 *
 * Not intended for use with injection factories.
 *
 * These need to have a longer lifecycle than [viewDisposeBag] but shorter than
 * [BaseViewModelFragment's][BaseViewModelFragment.disposeBag] or
 * [BaseViewModelActivity's][BaseViewModelActivity.disposeBag]. Thus, the precise
 * lifecycle can be passed in as the `whatDisposesAdapter` constructor
 * parameter.
 *
 * You may pass `null` in for `whatDisposesAdapter` if this object's
 * [disposeBag] is never used.
 *
 * Note: this mechanism will not invoke [onClearedListener] (at least not of writing).
 *
 * @author Philip Cohn-Cort (Fuzz)
 * @param whatDisposesAdapter this should be the same [DisposeBag] that is
 * responsible for cleaning up subscriptions on the associated adapter.
 */
abstract class CellViewModel(whatDisposesAdapter: DisposeBag?) : BaseViewModel() {

    init {
        whatDisposesAdapter?.add(disposeBag)
    }
}