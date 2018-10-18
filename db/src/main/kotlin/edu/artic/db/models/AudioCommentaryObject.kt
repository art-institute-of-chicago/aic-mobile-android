package edu.artic.db.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class AudioCommentaryObject(
        @Json(name = "object_selector_number") val objectSelectorNumber: String?,
        @Json(name = "audio") val audio: String?,
        var audioFile: ArticAudioFile?
) : Parcelable