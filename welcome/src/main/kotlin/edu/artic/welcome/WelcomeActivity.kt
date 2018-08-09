package edu.artic.welcome

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.navigation.Navigation
import edu.artic.base.utils.disableShiftMode
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.refreshPlayBackState
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_welcome.*
import kotlinx.android.synthetic.main.minimal_exo_playback_control_view.view.*

class WelcomeActivity : BaseActivity() {

    companion object {
        val EXTRA_QUIT: String = "EXTRA_QUIT"

        fun quitIntent(context: Context): Intent {
            val intent = Intent(context, WelcomeActivity::class.java)
            intent.putExtra(EXTRA_QUIT, true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }

    }

    var boundService: AudioPlayerService? = null
    var audioIntent: Intent? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerServiceBinder
            boundService = binder.getService()
            boundService?.let {
                audioPlayer.player = it.player
                it.player.refreshPlayBackState()
                audioPlayer.trackTitle.text = boundService?.articObject?.title
                audioPlayer.visibility = View.VISIBLE
            }
        }
    }

    override val layoutResId: Int
        get() = R.layout.activity_welcome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.extras?.getBoolean(EXTRA_QUIT) == true) {
            finish()
            return
        }
        audioIntent = Intent(this, AudioPlayerService::class.java)
        bottomNavigation.disableShiftMode(R.color.menu_color_list)
        bottomNavigation.selectedItemId = R.id.action_home
        bottomNavigation.setOnNavigationItemSelectedListener(NavigationSelectListener(this))
        Log.d("animation", "${audioPlayer.measuredHeight}")

        audioPlayer.closePlayer.setOnClickListener {
            audioPlayer.animate()
                    .yBy(200f)
                    .setDuration(600)
                    .withEndAction {
                        boundService?.stopPlayerService()
                        audioPlayer.visibility = View.INVISIBLE
                        audioPlayer.y = audioPlayer.y - 200f
                    }
                    .start()

        }

        audioPlayer.trackTitle.setOnClickListener {
            startActivity(boundService?.getIntent())
        }

    }

    override fun onResume() {
        super.onResume()
        bindService(audioIntent, serviceConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        unbindService(serviceConnection)
    }

    override fun onBackPressed() {
        if (!isTaskRoot && supportFragmentManager.backStackEntryCount == 0) {
            val navigationController = Navigation.findNavController(this, R.id.container)
            if (navigationController.currentDestination.id == R.id.welcomeFragment) {
                startActivity(quitIntent(this))
                finish()
                return
            }
        }
        super.onBackPressed()
    }
}