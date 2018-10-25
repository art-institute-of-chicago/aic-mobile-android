package edu.artic.info

import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.FragmentNavigator
import edu.artic.accesscard.AccessMemberCardFragment
import edu.artic.base.utils.disableShiftMode
import edu.artic.location.LocationService
import edu.artic.location.LocationServiceImpl
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationSelectListener
import edu.artic.navigation.linkHome
import edu.artic.ui.BaseActivity
import edu.artic.ui.findFragmentInHierarchy
import edu.artic.ui.findNavController
import kotlinx.android.synthetic.main.activity_info.*
import timber.log.Timber
import javax.inject.Inject

/**
 * # One of the four primary sections of the app.
 *
 * This Activity always hosts one full-size fragment, which must be one of
 * the following:
 * ## [InformationFragment] (default)
 * * In `:info` module
 * * Mostly just links to switch to one of the other Fragments
 * * Includes version code, credits, link to sign up for membership, etc.
 * ## [MuseumInformationFragment]
 * * In `:info` module
 * * Museum hours
 * * Museum location
 * * Contact info
 * * Link to buy tickets
 * ## [LanguageSettingsFragment][edu.artic.localization.ui.LanguageSettingsFragment]
 * * In `:localization_ui` module
 * * Change default application language
 * * Disclaimer about untranslated content
 * ## `InfoLocationSettingsFragment`
 * * In `:location_ui` module
 * * Grant or revoke location permissions
 * ## [AccessMemberCardFragment]
 * * In `:access_card` module
 * * Displays card metadata (if signed in)
 * * Displays sign-in form (if _not_ signed in)
 *
 * As a primary section, this class always contains a
 * [NarrowAudioPlayerFragment][edu.artic.media.ui.NarrowAudioPlayerFragment]
 * and a search icon.
 */
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

                val fm = supportFragmentManager

                val navController = fm.findNavController()

                // There is only value in continuing if we have a navController to use

                if (navController == null) {
                    if (BuildConfig.DEBUG) {
                        Timber.w("Info screen was asked to display card, but no navigation host could be found to perform that task.")
                    }
                } else {
                    val currentDestination = navController
                            .currentDestination

                    val argSelfImportant = getString(R.string.argSelfImportant)

                    /**
                     * Go to access member card iff current destination's label is not
                     * [R.string.accessMemberCardLabel] (that's the Label for
                     * [AccessMemberCardFragment]).
                     */
                    if (currentDestination?.id != R.id.accessMemberCardFragment) {

                        /**
                         * If the active fragment is not the start_destination navController can't find
                         * accessMemberCardLabel.
                         */
                        if (currentDestination?.id != R.id.informationFragment) {
                            navController.navigateUp()
                        }

                        navController.navigate(R.id.goToAccessMemberCard, Bundle().apply {
                            putBoolean(argSelfImportant, true)
                        })
                    } else if (currentDestination is FragmentNavigator.Destination) {

                        /**
                         * Ensure that the associated fragment will dismiss the activity when removed
                         */
                        findFragmentInHierarchy<AccessMemberCardFragment>(fm, R.id.container)?.let {
                            it.arguments?.putBoolean(argSelfImportant, true)
                        }
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
