package edu.artic.main


import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import edu.artic.base.utils.disableShiftMode
import edu.artic.viewmodel.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.reflect.KClass


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
        window.statusBarColor = Color.TRANSPARENT
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
    }

    private fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
        val win = activity.window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

}
