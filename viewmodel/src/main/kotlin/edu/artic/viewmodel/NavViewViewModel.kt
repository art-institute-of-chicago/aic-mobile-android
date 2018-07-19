package edu.artic.viewmodel

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

open class NavViewViewModel<T> : BaseViewModel() {

    val navigateTo: Subject<Navigate<T>> = PublishSubject.create<Navigate<T>>()
}