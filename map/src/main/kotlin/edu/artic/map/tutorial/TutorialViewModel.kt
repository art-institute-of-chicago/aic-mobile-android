package edu.artic.map.tutorial

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.map.R
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject

class TutorialViewModel @Inject constructor(private val tutorialPreferencesManager: TutorialPreferencesManager)
    : NavViewViewModel<TutorialViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    sealed class Stage {
        object One : Stage()
        object Two : Stage()

    }

    val cells: Subject<List<TutorialPopupItemViewModel>> = BehaviorSubject.createDefault(
            listOf(
                    TutorialPopupItemViewModel(R.drawable.arrows, R.string.tutorial_explore_text),
                    TutorialPopupItemViewModel(R.drawable.group_2, R.string.tutorial_audio_pins_text)
            )
    )

    val tutorialTitle: Subject<Int> = BehaviorSubject.create()

    /**
     * Reflection of what the 'previous' button's [alpha][android.view.View.ALPHA] should be.
     *
     * Defaults to `0f` (fully transparent)
     */
    val showBack: Subject<Float> = BehaviorSubject.createDefault(0f)

    val tutorialPopupCurrentPage: Subject<Int> = BehaviorSubject.createDefault(0)

    val currentTutorialStage: Subject<Stage> = BehaviorSubject.createDefault(Stage.One)

    init {
        tutorialPopupCurrentPage
                .map {
                    return@map if (it == 0)
                        R.string.tutorial_explore_title
                    else
                        R.string.tutorial_audio_pins_title
                }.bindTo(tutorialTitle)
                .disposedBy(disposeBag)
        tutorialPreferencesManager.hasSeenTutorial = true

    }

    fun onTutorialPageChanged(newPage: Int) {
        tutorialPopupCurrentPage.onNext(newPage)
    }

    fun onPopupNextClick() {
        val currentPage = tutorialPopupCurrentPage as BehaviorSubject
        if (currentPage.value == 0) {
            tutorialPopupCurrentPage.onNext(1)
        } else {
            (currentTutorialStage as BehaviorSubject).onNext(Stage.Two)
        }
    }

    fun onPopupBackClick() {
        // The only time that the back button will be available is if we are in the second screen
        tutorialPopupCurrentPage.onNext(0)
    }

    fun touched() {
        val stage = (currentTutorialStage as BehaviorSubject).value
        if (stage == Stage.Two) {
            navigateTo.onNext(Navigate.Back())
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("closedTutorial onClearedTutorialViewModel")
        tutorialPreferencesManager.hasClosedTutorialOnce = true
    }

}

class TutorialPopupItemViewModel(val imageId: Int, val textId: Int) : BaseViewModel()