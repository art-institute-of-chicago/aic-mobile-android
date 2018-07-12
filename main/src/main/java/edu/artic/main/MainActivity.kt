package edu.artic.main

import android.os.Bundle
import edu.artic.base.disableShiftMode

import edu.artic.viewmodel.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.reflect.KClass

class MainActivity : BaseViewModelActivity<MainViewModel>() {

    override val viewModelClass: KClass<MainViewModel>
        get() = MainViewModel::class

    override val layoutResId: Int
        get() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
    }
}
