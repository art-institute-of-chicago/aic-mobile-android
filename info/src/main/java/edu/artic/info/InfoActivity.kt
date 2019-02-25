package edu.artic.info

import android.content.Intent
import android.os.Bundle
import edu.artic.base.utils.disableShiftMode
import edu.artic.location.LocationService
import edu.artic.location.LocationServiceImpl
import edu.artic.navigation.NavigationSelectListener
import edu.artic.navigation.linkHome
import edu.artic.ui.BaseActivity
import edu.artic.ui.findNavController
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
            setOnNavigationItemReselectedListener {
                navController.popBackStack(R.id.informationFragment, false)
            }
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LocationServiceImpl.LOCATION_PERMISSION_REQUEST) {
            (locationService as LocationServiceImpl).onRequestPermissionsResult(requestCode, permissions, grantResults)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * DeepLinking does not work by default when activity is reordered to front.
     * So we need to handle it manually.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supportFragmentManager.findNavController()?.apply {
            /**
             * Normally, the root child fragment is [InformationFragment].
             * But when the user navigates from access_info_card link from WelcomeFragment, the root
             * fragment is [edu.artic.accesscard.AccessMemberCardFragment].
             *
             * We expect at most two fragment at a time in this graph.
             */
            popBackStack(R.id.informationFragment, false)
            onHandleDeepLink(intent)
        }
    }

    override fun onBackPressed() {
        if (isRootFragment(R.id.informationFragment)) {
            startActivity(linkHome())
            return
        }
        super.onBackPressed()
    }
}
