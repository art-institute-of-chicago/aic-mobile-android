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
}