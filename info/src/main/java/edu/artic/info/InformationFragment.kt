package edu.artic.info

import edu.artic.viewmodel.BaseViewModelFragment
import kotlin.reflect.KClass


class InformationFragment : BaseViewModelFragment<InformationViewModel>() {
    override val viewModelClass: KClass<InformationViewModel>
        get() = InformationViewModel::class

    override val title: String
        get() = "Information"

    override fun hasTransparentStatusBar(): Boolean = true

    override val layoutResId: Int
        get() = R.layout.fragment_information

}
