package edu.artic.base.utils.customTab

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsServiceConnection
import android.support.customtabs.CustomTabsSession
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import edu.artic.base.R
import edu.artic.base.utils.ActivityLifecycleAware

/**
 * @author Sameer Dhakal (Fuzz)
 */
class CustomTabManager : ActivityLifecycleAware {

    var mClient: CustomTabsClient? = null
    var mCustomTabsSession: CustomTabsSession? = null
    var mCustomTabsServiceConnection: CustomTabsServiceConnection? = null
    var customTabsIntent: CustomTabsIntent? = null
    var canBind: Boolean = false

    override fun onStop(host: Activity) {
        try {
            if (canBind) {
                host.unbindService(mCustomTabsServiceConnection)
            }
        } catch (ignore: Throwable) {
            // looks like this is thrown during fragment restart
        }
    }

    override fun onDestroy(host: Activity) {

    }

    override fun onStart(host: Activity) {

        try {
            // We want to support alternative CustomTabs packages, like chrome dev
            // and firefox, so we're not hardcoding a package name here.
            val packageName = CustomTabsHelper.getPackageNameToUse(host)
            canBind = !TextUtils.isEmpty(packageName)
            if (canBind) {
                CustomTabsClient.bindCustomTabsService(host, packageName, mCustomTabsServiceConnection)
            }
        } catch (t: Throwable) {
            // Package lookup failed. Nothing to be done.
            canBind = false
        }

    }

    override fun onCreate(host: Activity) {

    }

    /**
     * Launches custom tab
     * @param context {@link Context}
     * @param uri {@link Uri} Web Url
     */
    fun openUrlOnChromeCustomTab(context: Context, uri: Uri) {
        val colorId = ContextCompat.getColor(context, R.color.colorPrimaryDark)

        mCustomTabsServiceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(componentName: ComponentName,
                                                      customTabsClient: CustomTabsClient) {
                mClient = customTabsClient
                mClient?.warmup(0L)
                mCustomTabsSession = mClient?.newSession(null)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
            }
        }

        customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
                .addDefaultShareMenuItem()
                .setToolbarColor(colorId)
                .setShowTitle(true)
                .build()

        customTabsIntent?.launchUrl(context, uri)
    }

    fun isCustomChromeTabSupported(host: Context): Boolean {
        return CustomTabsHelper.getPackageNameToUse(host) != null
    }
}