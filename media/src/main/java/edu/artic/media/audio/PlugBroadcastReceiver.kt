package edu.artic.media.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

/**
 * Receiver for [AudioManager.ACTION_HEADSET_PLUG] events.
 *
 * [Register][Context.registerReceiver] one of these to run code
 * whenever a headset is plugged in or unplugged.
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
class PlugBroadcastReceiver(
        private val onUnplug: Runnable,
        private val onPlugIn: Runnable
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        intent?.let {
            if (it.action == AudioManager.ACTION_HEADSET_PLUG) {
                when (it.getIntExtra("state", -1)) {
                    0 -> onUnplug.run()
                    1 -> onPlugIn.run()
                }
            }
        }
    }

}
