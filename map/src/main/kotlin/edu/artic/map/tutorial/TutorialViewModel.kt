package edu.artic.map.tutorial

import edu.artic.map.R
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class TutorialViewModel @Inject constructor() : BaseViewModel() {

    val cells: Subject<List<TutorialPopupItemViewModel>> = BehaviorSubject.createDefault(
            listOf(
                    TutorialPopupItemViewModel(R.drawable.arrows, R.string.tutorial_explore_text),
                    TutorialPopupItemViewModel(R.drawable.group_2, R.string.tutorial_audio_pins_text)
            )
    )

}

class TutorialPopupItemViewModel(val imageId: Int, val textId: Int) : BaseViewModel()