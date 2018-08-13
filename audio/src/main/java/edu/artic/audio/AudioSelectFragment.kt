package edu.artic.audio

import edu.artic.analytics.ScreenCategoryName
import edu.artic.ui.BaseFragment


class AudioSelectFragment : BaseFragment() {
    override val title: String
        get() = ""
    override val layoutResId: Int
        get() = R.layout.fragment_audio_select

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.AudioPlayer

}
