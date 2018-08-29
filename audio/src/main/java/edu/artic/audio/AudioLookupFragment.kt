package edu.artic.audio

import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlin.reflect.KClass

/**
 * This is the basis for a screen that finds [ArticAudioFile]s for by id.
 *
 * We want to support lookup of any String that can map to a [ArticAudioFile.nid].
 * At current moment, all of these are numeric and so the UI displayed by this
 * class is styled after a numeric keyboard.
 *
 * Business logic (like the actual lookup) is handled in [AudioLookupViewModel].
 * This class is hosted in an [AudioActivity].
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
class AudioLookupFragment : BaseViewModelFragment<AudioLookupViewModel>() {

    override val viewModelClass: KClass<AudioLookupViewModel>
        get() = AudioLookupViewModel::class
    override val title: String
        get() = ""
    override val layoutResId: Int
        get() = R.layout.fragment_audio_lookup
    override val screenCategory: ScreenCategoryName?
        get() = ScreenCategoryName.AudioGuide



}
