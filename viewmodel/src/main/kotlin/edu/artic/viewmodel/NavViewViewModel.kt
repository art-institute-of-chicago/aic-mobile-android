package edu.artic.viewmodel

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

open class NavViewViewModel<T> : BaseViewModel() {

    /**
     * # Very important:
     *
     * If this field is observed in a [BaseViewModelFragment.setupNavigationBindings],
     * you'd better dispose it with [BaseViewModelFragment.navigationDisposeBag].
     *
     * Exceptions to this rule may be permitted on a case by case basis. **Failure
     * to comply may result in duplicate navigation events (and thus 'id not found'
     * crashes)**
     */
    val navigateTo: Subject<Navigate<T>> = PublishSubject.create<Navigate<T>>()
}