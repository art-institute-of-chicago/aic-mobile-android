package edu.artic.localization

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

// We will want our analytics in english, and therefore will always pull these constants
private const val english = "English"
private const val spanish = "Spanish"
private const val chinese = "Chinese"
private const val korean = "Korean"
private const val french = "French"

interface SpecifiesLanguage {

    fun underlyingLanguage(): String?

    fun underlyingLocale(): Locale {
        return underlyingLanguage()?.asLanguageTag().orFallback(Locale.ROOT)
    }

    /**
     * Alias to [Locale.forLanguageTag].
     *
     * The SDK function's javadoc says that it'll throw a NullPointerException
     * if its parameter is null, so we disallow null receivers here.
     */
    private fun String.asLanguageTag(): Locale {
        return Locale.forLanguageTag(this)
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
        ).getText(R.string.global_current_language)
    }

    /**
     * Retrieve the name of [underlyingLocale]'s language under the given Context's
     * config.
     */
    fun userFriendlyLanguage(ctx: Context): CharSequence {
        return when (underlyingLocale().language) {
            Locale.ENGLISH.language -> ctx.getText(R.string.localization_english)
            SPANISH.language -> ctx.getText(R.string.localization_spanish)
            Locale.CHINESE.language -> ctx.getText(R.string.localization_chinese)
            Locale.KOREAN.language -> ctx.getText(R.string.localization_korean)
            Locale.FRENCH.language -> ctx.getText(R.string.localization_french)
            else -> underlyingLocale().displayLanguage
        }
    }

    fun fileLanguageForAnalytics(): CharSequence {
        return when (underlyingLocale().language) {
            Locale.ENGLISH.language -> english
            SPANISH.language -> spanish
            Locale.CHINESE.language -> chinese
            Locale.KOREAN.language -> korean
            Locale.FRENCH.language -> french
            else -> underlyingLocale().displayLanguage
        }
    }
}
