package edu.artic.db.models

data class ArticGeneralInfo(
        val title: String,
        val status: String,
        val nid: String,
        val translations: List<Translation>,
        val museum_hours: String,
        val home_member_prompt_text: String,
        val audio_title: String,
        val audio_subtitle: String,
        val map_title: String,
        val map_subtitle: String,
        val info_title: String,
        val info_subtitle: String,
        val gift_shops_title: String,
        val gift_shops_text: String,
        val members_lounge_title: String,
        val members_lounge_text: String,
        val see_all_tours_intro: String,
        val restrooms_title: String,
        val restrooms_text: String

) {

    data class Translation(
            val language: String,
            val museum_hours: String,
            val home_member_prompt_text: String,
            val audio_title: String,
            val audio_subtitle: String,
            val map_title: String,
            val map_subtitle: String,
            val info_title: String,
            val info_subtitle: String,
            val gift_shops_title: String,
            val gift_shops_text: String,
            val members_lounge_title: String,
            val members_lounge_text: String,
            val see_all_tours_intro: String,
            val restrooms_title: String,
            val restrooms_text: String
    )
}