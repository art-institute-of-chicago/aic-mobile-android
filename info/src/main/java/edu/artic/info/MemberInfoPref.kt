package edu.artic.info

import android.content.Context
import edu.artic.base.BasePreferencesManager

/**
 * @author Sameer Dhakal (Fuzz)
 */

class MemberInfoPreferencesManager(context: Context)
    : BasePreferencesManager(context, "memberInfo") {

    var memberZipCode: String?
        set(value) = putString("memberZIP", value)
        get() = getString("memberZIP")

    var memberID: String?
        set(value) = putString("memberID", value)
        get() = getString("memberID")
}