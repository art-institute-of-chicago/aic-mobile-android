package edu.artic.main


import android.os.Bundle
import android.support.v4.app.FragmentTransaction
import android.view.WindowManager
import edu.artic.base.utils.disableShiftMode

import edu.artic.viewmodel.BaseViewModelActivity
import edu.artic.welcome.WelcomeFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.reflect.KClass
import android.app.Activity
import android.graphics.Color


/**
 * This Activity is responsible for the bottom navigation menu.
 *
 */
class MainActivity : BaseViewModelActivity<MainViewModel>() {

    override val viewModelClass: KClass<MainViewModel>
        get() = MainViewModel::class

    override val layoutResId: Int
        get() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window.statusBarColor = Color.TRANSPARENT;
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
        loadContainerFragment(WelcomeFragment())
    }

    fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
        val win = activity.window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    /**
     * Loads the provided @containerFragment into container
     */
    private fun loadContainerFragment(containerFragment: WelcomeFragment) {
        val fragmentName: String = containerFragment.javaClass.simpleName
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        if (container.childCount > 1) {
            transaction.replace(R.id.container, containerFragment)
        } else {
            transaction.add(R.id.container, containerFragment)
        }
        transaction.addToBackStack(fragmentName)
                .commit()
    }
}
