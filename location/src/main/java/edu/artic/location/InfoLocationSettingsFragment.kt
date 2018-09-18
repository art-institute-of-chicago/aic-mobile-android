package edu.artic.location

import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlin.reflect.KClass

class InfoLocationSettingsFragment : BaseViewModelFragment<InfoLocationSettingsViewModel>() {
    override val viewModelClass: KClass<InfoLocationSettingsViewModel> = InfoLocationSettingsViewModel::class
    override val title: String = requireContext().getString(R.string.location_title)
    override val layoutResId: Int = 0
    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.LocationSettings
}