package edu.artic.map

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.disableShiftMode
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.refreshPlayBackState
import edu.artic.navigation.NavigationSelectListener
import edu.artic.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.minimal_exo_playback_control_view.view.*

class MapActivity : BaseActivity() {

    override val layoutResId: Int
        get() = R.layout.activity_map

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
                audioPlayer.trackTitle.text = boundService?.audioObject?.title
                audioPlayer.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioIntent = Intent(this, AudioPlayerService::class.java)
        bottomNavigation.apply {
            disableShiftMode(R.color.map_menu_color_list)
            selectedItemId = R.id.action_map
            setOnNavigationItemSelectedListener(NavigationSelectListener(this.context))
        }

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
            startActivity("edu.artic.audio".asDeepLinkIntent())
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

}