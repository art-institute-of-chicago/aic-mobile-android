package edu.artic.info


import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_information.*
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
