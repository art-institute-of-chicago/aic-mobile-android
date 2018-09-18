package edu.artic.location

import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlin.reflect.KClass

class InfoLocationSettingsFragment : BaseViewModelFragment<InfoLocationSettingsViewModel>() {
    override val viewModelClass: KClass<InfoLocationSettingsViewModel> = InfoLocationSettingsViewModel::class
    override val title: String = "Location Settings"
    override val layoutResId: Int = R.layout.fragment_location_settings
    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.LocationSettings


}