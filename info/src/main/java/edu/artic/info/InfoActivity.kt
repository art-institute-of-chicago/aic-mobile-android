package edu.artic.info

import android.os.Bundle
import edu.artic.navigation.NavigationSelectListener
import edu.artic.base.utils.disableShiftMode
import edu.artic.base.utils.preventReselection
import edu.artic.location.LocationService
import edu.artic.location.LocationServiceImpl
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_info.*
import javax.inject.Inject

class InfoActivity : BaseActivity() {

    /**
     * locationService is here so it gets generated before the onResume flow happens
     */
    @Inject
    lateinit var locationService: LocationService

    override val layoutResId: Int
        get() = R.layout.activity_info

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomNavigation.apply {
            disableShiftMode(R.color.info_menu_color_list)
            selectedItemId = R.id.action_info
            preventReselection()
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == LocationServiceImpl.LOCATION_PERMISSION_REQUEST){
            (locationService as LocationServiceImpl).onRequestPermissionsResult(requestCode, permissions, grantResults)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
