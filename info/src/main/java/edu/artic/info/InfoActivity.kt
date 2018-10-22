package edu.artic.info

import android.content.Intent
import android.os.Bundle
import edu.artic.base.utils.disableShiftMode
import edu.artic.location.LocationService
import edu.artic.location.LocationServiceImpl
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationSelectListener
import edu.artic.navigation.linkHome
import edu.artic.ui.BaseActivity
import edu.artic.ui.findNavController
import kotlinx.android.synthetic.main.activity_info.*
import timber.log.Timber
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        /**
         * DeepLinking does not work by default when activity is reordered to front.
         * So we need to handle it manually.
         */
        when (intent.data?.toString()?.replace("artic://", "")) {
            NavigationConstants.INFO_MEMBER_CARD -> {

                val navController = supportFragmentManager.findNavController()

                // There is only value in continuing if we have a navController to use

                if (navController == null) {
                    if (BuildConfig.DEBUG) {
                        Timber.w("Info screen was asked to display card, but no navigation host could be found to perform that task.")
                    }
                } else {
                    val currentDestination = navController
                            .currentDestination
                            ?.label
                            ?.toString()

                    /**
                     * Go to access member card iff current destination's label is not [R.string.accessMemberCardLabel].
                     * Label for [AccessMemberCardFragment] is [R.string.accessMemberCardLabel].
                     */
                    if (currentDestination != resources.getString(R.string.accessMemberCardLabel)) {

                        /**
                         * If the active fragment is not the start_destination navController can't find
                         * accessMemberCardLabel.
                         */
                        if (currentDestination != resources.getString(R.string.fragmentInformationLabel)) {
                            navController.navigateUp()
                        }

                        navController.navigate(R.id.goToAccessMemberCard)
                    }
                }
            }

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
