package edu.artic.search

import edu.artic.viewmodel.NavViewViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

//Manager
class SearchResultsContainerViewModel @Inject constructor() : NavViewViewModel<SearchResultsContainerViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    val currentlySelectedPage : Subject<Int> = BehaviorSubject.create()

    fun onPageChanged(page : Int) {
        currentlySelectedPage.onNext(page)
    }

    fun onClickSeeAll(header: Header) {
        when(header) {
            is Header.Artworks -> {
                currentlySelectedPage.onNext(1)
            }
            is Header.Tours -> {
                currentlySelectedPage.onNext(2)
            }
            is Header.Exhibitions -> {
                currentlySelectedPage.onNext(3)
            }
        }
    }

}
