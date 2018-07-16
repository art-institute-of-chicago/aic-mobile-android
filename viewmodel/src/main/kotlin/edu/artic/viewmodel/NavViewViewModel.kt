package edu.artic.viewmodel

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

open class NavViewViewModel<T> : BaseViewModel() {

    val navigateTo :Subject<Navigate<T>> = BehaviorSubject.create<Navigate<T>>()
}