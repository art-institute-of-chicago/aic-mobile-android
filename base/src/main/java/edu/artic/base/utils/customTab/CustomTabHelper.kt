// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.artic.base.utils.customTab

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import java.util.*

/**
 * Helper class for Custom Tabs.
 *
 *
 * File slightly modified from Chromium project class
 * at `https://github.com/GoogleChrome/custom-tabs-client/blob/addb26ad8fce8cdcb640217f4c9da75dd19763cc/shared/src/main/java/org/chromium/customtabsclient/shared/CustomTabsHelper.java`
 *
 * @author Sameer Dhakal (Fuzz)
 */
class CustomTabsHelper {
    companion object {
        val TAG = "CustomTabsHelper"
        val STABLE_PACKAGE = "com.android.chrome"
        val BETA_PACKAGE = "com.chrome.beta"
        val DEV_PACKAGE = "com.chrome.dev"
        val LOCAL_PACKAGE = "com.google.android.apps.chrome"
        val ACTION_CUSTOM_TABS_CONNECTION = "android.support.customtabs.action.CustomTabsService"


        var sPackageNameToUse: String? = null

        /**
         * @return All possible chrome package names that provide custom tabs feature.
         */
        val packages: Array<String>
            get() = arrayOf("", STABLE_PACKAGE, BETA_PACKAGE, DEV_PACKAGE, LOCAL_PACKAGE)

        /**
         * Goes through all apps that 1. handle VIEW intents and 2. have a warmup service. Picks
         * the one chosen by the user if there is one, otherwise makes a best effort to return a
         * valid package name.
         *
         * This is **not** threadsafe.
         *
         * @param context [Context] to use for accessing [PackageManager].
         * @return The package name recommended to use for connecting to custom tabs related components.
         */
        fun getPackageNameToUse(context: Context): String? {
            if (sPackageNameToUse != null) return sPackageNameToUse

            val pm = context.packageManager
            // Get default VIEW intent handler.
            val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
            val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
            var defaultViewHandlerPackageName: String? = null
            if (defaultViewHandlerInfo != null) {
                defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName
            }

            // Get all apps that can handle VIEW intents.
            val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
            val packagesSupportingCustomTabs = ArrayList<String>()
            for (info in resolvedActivityList) {
                val serviceIntent = Intent()
                serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
                serviceIntent.`package` = info.activityInfo.packageName
                if (pm.resolveService(serviceIntent, 0) != null) {
                    packagesSupportingCustomTabs.add(info.activityInfo.packageName)
                }
            }

            // Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
            // and service calls.
            if (packagesSupportingCustomTabs.isEmpty()) {
                sPackageNameToUse = null
            } else if (packagesSupportingCustomTabs.size == 1) {
                sPackageNameToUse = packagesSupportingCustomTabs[0]
            } else if (!TextUtils.isEmpty(defaultViewHandlerPackageName)
                    && !hasSpecializedHandlerIntents(context, activityIntent)
                    && packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)) {
                sPackageNameToUse = defaultViewHandlerPackageName
            } else if (packagesSupportingCustomTabs.contains(STABLE_PACKAGE)) {
                sPackageNameToUse = STABLE_PACKAGE
            } else if (packagesSupportingCustomTabs.contains(BETA_PACKAGE)) {
                sPackageNameToUse = BETA_PACKAGE
            } else if (packagesSupportingCustomTabs.contains(DEV_PACKAGE)) {
                sPackageNameToUse = DEV_PACKAGE
            } else if (packagesSupportingCustomTabs.contains(LOCAL_PACKAGE)) {
                sPackageNameToUse = LOCAL_PACKAGE
            }
            return sPackageNameToUse
        }

        /**
         * Used to check whether there is a specialized handler for a given intent.
         * @param intent The intent to check with.
         * @return Whether there is a specialized handler for the given intent.
         */
        private fun hasSpecializedHandlerIntents(context: Context, intent: Intent): Boolean {
            try {
                val pm = context.packageManager
                val handlers = pm.queryIntentActivities(
                        intent,
                        PackageManager.GET_RESOLVED_FILTER)
                if (handlers == null || handlers.size == 0) {
                    return false
                }
                for (resolveInfo in handlers) {
                    val filter = resolveInfo.filter ?: continue
                    if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue
                    if (resolveInfo.activityInfo == null) continue
                    return true
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Runtime exception while getting specialized handlers")
            }

            return false
        }
    }
}