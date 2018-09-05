package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.localization.SpecifiesLanguage

@JsonClass(generateAdapter = true)
@Entity
data class ArticGeneralInfo(
        @Json(name = "title") val title: String,
        @Json(name = "status") val status: String,
        @Json(name = "nid") @PrimaryKey val nid: String,
        @Json(name = "translations") val translations: List<Translation>,
        @Json(name = "museum_hours") val museumHours: String,
        @Json(name = "home_member_prompt_text") val homeMemberPromptText: String,
        @Json(name = "audio_title") val audioTitle: String,
        @Json(name = "audio_subtitle") val audioSubtitle: String,
        @Json(name = "map_title") val mapTitle: String,
        @Json(name = "map_subtitle") val mapSubtitle: String,
        @Json(name = "info_title") val infoTitle: String,
        @Json(name = "info_subtitle") val infoSubtitle: String,
        @Json(name = "gift_shops_title") val giftShopsTitle: String,
        @Json(name = "gift_shops_text") val gift_shops_text: String,
        @Json(name = "members_lounge_title") val membersLoungeTitle: String,
        @Json(name = "members_lounge_text") val membersLoungeText: String,
        @Json(name = "see_all_tours_intro") val seeAllToursIntro: String,
        @Json(name = "restrooms_title") val restroomsTitle: String,
        @Json(name = "restrooms_text") val restroomsText: String

) {

    data class Translation(
            val language: String,
            @Json(name = "museum_hours") val museumHours: String,
            @Json(name = "home_member_prompt_text") val homeMemberPromptText: String,
            @Json(name = "audio_title") val audioTitle: String,
            @Json(name = "audio_subtitle") val audioSubtitle: String,
            @Json(name = "map_title") val mapTitle: String,
            @Json(name = "map_subtitle") val mapSubtitle: String,
            @Json(name = "info_title") val infoTitle: String,
            @Json(name = "info_subtitle") val infoSubtitle: String,
            @Json(name = "gift_shops_title") val giftShopsTitle: String,
            @Json(name = "gift_shops_text") val gift_shops_text: String,
            @Json(name = "members_lounge_title") val membersLoungeTitle: String,
            @Json(name = "members_lounge_text") val membersLoungeText: String,
            @Json(name = "see_all_tours_intro") val seeAllToursIntro: String,
            @Json(name = "restrooms_title") val restroomsTitle: String,
            @Json(name = "restrooms_text") val restroomsText: String
    ) : SpecifiesLanguage {
        override fun underlyingLanguage(): String? {
            return language
        }
    }

    /**
     * Retrieve _all_ of the translations of this content, in one
     * ordered list.
     *
     * Note that this uses the (safe) assumption that [ArticGeneralInfo]
     * itself is an English translation of the content.
     *
     * @see ArticAudioFile.allTranslations
     */
    fun allTranslations(): List<Translation> {
        return translations.mapTo(mutableListOf(asTranslation())) { it }
    }

    /**
     * Convert this GeneralInfo into a [Translation], with specific
     * language of `en-US`. Intended for use only by [allTranslations].
     */
    private fun asTranslation(): Translation {
        return Translation(
                language = "en-US",
                museumHours = museumHours,
                homeMemberPromptText = homeMemberPromptText,
                audioTitle = audioTitle,
                audioSubtitle = audioSubtitle,
                mapTitle = mapTitle,
                mapSubtitle = mapSubtitle,
                infoTitle = infoTitle,
                infoSubtitle = infoSubtitle,
                giftShopsTitle = giftShopsTitle,
                gift_shops_text = gift_shops_text,
                membersLoungeTitle = membersLoungeTitle,
                membersLoungeText = membersLoungeText,
                seeAllToursIntro = seeAllToursIntro,
                restroomsTitle = restroomsTitle,
                restroomsText = restroomsText
        )
    }
}