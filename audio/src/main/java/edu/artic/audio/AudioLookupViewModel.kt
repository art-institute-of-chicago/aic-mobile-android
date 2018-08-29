package edu.artic.audio

import edu.artic.audio.NumberPadElement.*
import edu.artic.viewmodel.BaseViewModel
import javax.inject.Inject

/**
 * This class provides important audio-related logic to an [AudioLookupFragment].
 *
 * For the ViewModel shown for details about a single audio file, check out
 * [AudioDetailsViewModel] instead.
 */
class AudioLookupViewModel @Inject constructor() : BaseViewModel() {

    /**
     * See [NumberPadAdapter] for details on all this.
     */
    val preferredNumberPadElements: List<NumberPadElement> = listOf(
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
            Numeric("0"),
            GoSearch
    )

}
