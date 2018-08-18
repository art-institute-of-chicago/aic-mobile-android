package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.localization.BaseTranslation
import kotlinx.android.parcel.Parcelize

/**
 * Audio information for an [ArticObject].
 *
 * This includes the network url for that audio, as well as
 * a transcript and [credits]. By default, this is only available
 * in english; other translations for the same [ArticObject] are
 * included under [ArticAudioFile.translations].
 */
@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticAudioFile(
        @Json(name = "title") val title: String?,
        @Json(name = "status") val status: String?,
        @Json(name = "nid") @PrimaryKey val nid: String,
        @Json(name = "type") val type: String?,
        @Json(name = "translations") val translations: List<Translation>,
        @Json(name = "audio_filename") val fileName: String?,
        @Json(name = "audio_file_url") val fileUrl: String?,
        @Json(name = "audio_filemime") val fileMime: String?,
        @Json(name = "audio_filesize") val fileSize: String?,
        @Json(name = "audio_transcript") val transcript: String?,
        @Json(name = "credits") val credits: String?,
        @Json(name = "track_title") val trackTitle: String?
) : Parcelable {
    @JsonClass(generateAdapter = true)
    @Parcelize
    data class Translation(
            @Json(name = "language") val language: String?,
            @Json(name = "title") val title: String?,
            @Json(name = "track_title") val trackTitle: String?,
            @Json(name = "audio_filename") val fileName: String?,
            @Json(name = "audio_file_url") val fileUrl: String?,
            @Json(name = "audio_filemime") val fileMime: String?,
            @Json(name = "audio_filesize") val fileSize: String?,
            @Json(name = "audio_transcript") val transcript: String?,
            @Json(name = "credits") val credits: String?
    ) : Parcelable, BaseTranslation {
        override fun underlyingLanguage(): String? {
            return language
        }
    }

    /**
     * Retrieve _all_ of the translations of this content, in one
     * ordered list.
     *
     * Note that this uses the (safe) assumption that [ArticAudioFile]
     * itself is an English translation of the content.
     */
    fun allTranslations() : List<AudioTranslation> {
        return translations.mapTo(mutableListOf(asAudioTranslation())) {
            it.asAudioTranslation()
        }
    }
}


fun ArticAudioFile.asAudioTranslation() : AudioTranslation {
    return AudioTranslation(
            "en-US",
            title,
            fileName,
            fileUrl,
            fileMime,
            fileSize,
            transcript,
            credits
    )
}

fun ArticAudioFile.Translation.asAudioTranslation() : AudioTranslation {
    return AudioTranslation(
            language,
            title,
            fileName,
            fileUrl,
            fileMime,
            fileSize,
            transcript,
            credits
    )
}

/**
 * This object offers easy access to the common fields between [ArticAudioFile] and
 * [ArticAudioFile.Translation].
 *
 * If this were a superclass of each of those two types, Moshi would get really
 * confused. Perhaps in a later version the auto-generation will be able to
 * handle that?
 */
data class AudioTranslation(
        val language: String?,
        val title: String?,
        val fileName: String?,
        val fileUrl: String?,
        val fileMime: String?,
        val fileSize: String?,
        val transcript: String?,
        val credits: String?
) : BaseTranslation {
    override fun underlyingLanguage(): String? {
        return language
    }
}
