package edu.artic.localization

import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.*

/**
 * Simple proxy to [Configuration.getLocales] or
 * [Configuration.locale], depending on SDK version.
 */
var Configuration.primaryLocale: Locale
    set(v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (locales.indexOf(v) == -1) {
                val toAssign = locales.asKotlinList()
                toAssign.add(0, v)
                locales = LocaleList(*toAssign.toTypedArray())
            }
        } else {
            setLocale(v)
        }
    }
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locales[0]
        } else {
            locale
        }
    }
