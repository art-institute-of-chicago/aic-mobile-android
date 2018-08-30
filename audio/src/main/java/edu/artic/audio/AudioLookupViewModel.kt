package edu.artic.audio

import edu.artic.audio.NumberPadElement.*
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * This class provides important audio-related logic to an [AudioLookupFragment].
 *
 * For the ViewModel shown for details about a single audio file, check out
 * [AudioDetailsViewModel] instead.
 */
class AudioLookupViewModel @Inject constructor() : BaseViewModel() {

    val adapterClicks: Subject<NumberPadElement> = PublishSubject.create()

    /**
     * See [NumberPadAdapter] for details on all this.
     */
    val preferredNumberPadElements: Subject<List<NumberPadElement>> = BehaviorSubject.createDefault(listOf(
            Numeric("1"),
            Numeric("2"),
            Numeric("3"),
            Numeric("4"),
            Numeric("5"),
            Numeric("6"),
            Numeric("7"),
            Numeric("8"),
            Numeric("9"),
            DeleteBack,
            // NB: Due to a bug in the ideal_sans_medium font files, the 0 and o look very similar. This is a zero.
            Numeric("0"),
            GoSearch
    ))

}
