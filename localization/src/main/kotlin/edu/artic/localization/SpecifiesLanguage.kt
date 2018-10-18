package edu.artic.localization;

import android.content.Context
import android.content.res.Configuration
import java.util.*

/**
 * Common interface for DAO models which can be treated like translations of
 * some sort of media.
 *
 * This must be an `interface` and not a proper superclass due
 * to [limitations in Moshi][com.squareup.moshi.JsonClass].
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
interface SpecifiesLanguage {

    fun underlyingLanguage() : String?

    fun underlyingLocale(): Locale {
        return Locale.forLanguageTag(underlyingLanguage())
    }

    /**
     * Retrieve the name of [underlyingLocale]'s language in that language.
     */
    private fun nameOfLanguageInThatLanguage(ctx: Context): CharSequence {
        val current = ctx.resources.configuration

        return ctx.createConfigurationContext(
                Configuration(current).apply {
                    // Locale retrieval on pre-Nougat is somewhat lacking
                    setLocale(Locale.forLanguageTag(underlyingLocale().language))
                }
        ).getText(R.string.name_of_this_language)
    }

    /**
     * Retrieve the name of [underlyingLocale]'s language under the given Context's
     * config.
     */
    fun userFriendlyLanguage(ctx: Context): CharSequence {
        return when(underlyingLocale().language) {
            Locale.ENGLISH.language -> ctx.getText(R.string.english)
            SPANISH.language -> ctx.getText(R.string.spanish)
            Locale.CHINESE.language -> ctx.getText(R.string.chinese)
            else -> underlyingLocale().displayLanguage
        }
    }
}
