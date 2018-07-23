package edu.artic.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_info.*

class InfoActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_info

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        bottomNavigation.disableShiftMode(R.color.menu_color_list)
        bottomNavigation.selectedItemId = R.id.action_info
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("artic://com.artic.home"))
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
