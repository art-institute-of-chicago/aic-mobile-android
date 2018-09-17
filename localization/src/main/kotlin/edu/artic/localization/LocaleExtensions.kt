package edu.artic.localization

import android.annotation.TargetApi
import android.os.Build
import android.os.LocaleList
import java.util.*


/**
 * Switch to a given `fallback` if this object doesn't have a language.
 *
 * Call this only if we're doing some sort of locale-aware work; for
 * API calls, you should always be hardcoding the Locale you agreed
 * upon with the other party.
 *
 * @param fallback (optional) something with the language to use if
 * we don't have one
 * @return this object or `fallback` as described above
 */
fun Locale?.orFallback(fallback: Locale = Locale.ENGLISH): Locale {
    return if (this == null || this.hasNoLanguage()) {
        fallback
    } else {
        this
    }
}

/**
 * Returns the name of this language, as used by our analytics engine.
 *
 * We use short-cut logic for known values, and fallback to [Locale.getDisplayLanguage]
 * otherwise.
 */
fun Locale.nameOfLanguageForAnalytics(): String {
    return when (language) {
        Locale.ENGLISH.language -> "English"
        SPANISH.language -> "Spanish"
        Locale.CHINESE.language -> "Chinese"
        else -> getDisplayLanguage(Locale.ENGLISH)
    }
}

val SPANISH: Locale
    get() = Locale.forLanguageTag("es")

/**
 * Converts this [LocaleList] into a standard [MutableList].
 */
@TargetApi(Build.VERSION_CODES.N)
fun LocaleList.asKotlinList(): MutableList<Locale> {
    val asList = mutableListOf<Locale>()
    for (i in 0 until size()) {
        asList.add(this[i])
    }
    return asList
}

/**
 * Check whether our [language][Locale.toLanguageTag] is the 'undefined' constant.
 */
fun Locale.hasNoLanguage() =
        this.language.isEmpty() || this.toLanguageTag() == "und"