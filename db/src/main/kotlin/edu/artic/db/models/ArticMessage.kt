package edu.artic.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.localization.SpecifiesLanguage
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticMessage(
        @PrimaryKey var nid: String = "",
        @Json(name = "title") val title: String?,
        @Json(name = "message_type") val messageType: String?,
        @Json(name = "expiration_threshold") val expirationThreshold: Int?,
        @Json(name = "tour_exit") val tourExit: String?,
        @Json(name = "persistent") val isPersistent: Boolean?,
        @Json(name = "message") val message: String?,
        @Json(name = "action") val action: String?,
        @Json(name = "action_title") val actionTitle: String?,
        @Json(name = "translations") val translations: List<Translation>
) : Parcelable {

    @JsonClass(generateAdapter = true)
    @Parcelize
    data class Translation(
            @Json(name = "language") val language: String?,
            @Json(name = "title") val title: String?,
            @Json(name = "message") val message: String?,
            @Json(name = "action_title") val actionTitle: String?
    ) : Parcelable, SpecifiesLanguage {
        override fun underlyingLanguage(): String? = language
    }

    /**
     * Returns the translations including english language
     */
    val allTranslations: List<ArticMessage.Translation>
        get() {
            return mutableListOf(toEnglishTranslation()).apply { addAll(translations) }
        }

}

/**
 * Builds english translation for [ArticMessage].
 * [ArticMessage]'s properties are in english, so copying the required properties suffice the english translation.
 */
fun ArticMessage.toEnglishTranslation(): ArticMessage.Translation {
    return ArticMessage.Translation(
            language = "en-US",
            title = title,
            message = message,
            actionTitle = actionTitle
    )
}
