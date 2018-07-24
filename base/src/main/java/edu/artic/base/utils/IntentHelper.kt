package edu.artic.base.utils

import android.content.Intent
import android.net.Uri

/**
 *@author Sameer Dhakal (Fuzz)
 */


fun Intent.fromUriString(uriString: String, action: String = Intent.ACTION_VIEW, schema: String = "artic"): Intent {
    return Intent(action, Uri.parse("$schema://$uriString"))
}