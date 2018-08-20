package edu.artic.media.audio

import edu.artic.db.models.ArticAudioFile
import edu.artic.db.models.AudioTranslation
import edu.artic.localization.LanguageSelector

/**
 * Obtain a reasonable [AudioTranslation] from this audio commentary.
 *
 * Note that the primary audio is English, as explained in further depth
 * at the docs for [AudioTranslation].
 */
fun ArticAudioFile.preferredLanguage(selector: LanguageSelector) : AudioTranslation {
    return selector.selectFrom(this.allTranslations())
}