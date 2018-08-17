package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.localization.BaseTranslation
import kotlinx.android.parcel.Parcelize

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
    ) : Parcelable, BaseTranslation
}