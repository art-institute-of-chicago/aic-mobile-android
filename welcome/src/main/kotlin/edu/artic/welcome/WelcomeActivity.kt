package edu.artic.welcome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import edu.artic.ui.BaseActivity
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.BaseViewModelActivity
import kotlinx.android.synthetic.main.activity_welcome.*
import javax.inject.Inject
import kotlin.reflect.KClass

class WelcomeActivity : BaseActivity() {


    override val layoutResId: Int
        get() = R.layout.activity_welcome


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //bottomNavigation.disableShiftMode(R.color.menu_color_list)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_info -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("artic://com.artic.info"))
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }
}