package edu.artic.media.audio

import edu.artic.db.models.ArticAudioFile
import edu.artic.db.models.AudioFileModel
import edu.artic.localization.LanguageSelector

/**
 * Obtain a reasonable [AudioFileModel] from this audio commentary.
 *
 * Note that the primary audio is English, as explained in further depth
 * at the docs for [AudioFileModel].
 */
fun ArticAudioFile.preferredLanguage(selector: LanguageSelector) : AudioFileModel {
    return selector.selectFrom(this.allTranslations())
}