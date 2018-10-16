package edu.artic.navigation

import android.content.Intent
import edu.artic.base.utils.asDeepLinkIntent

/**
 *@author Sameer Dhakal (Fuzz)
 */

class NavigationConstants {
    companion object {
        const val HOME: String = "edu.artic.home"
        const val MAP: String = "edu.artic.map"
        const val SEARCH: String = "edu.artic.search"
        const val AUDIO: String = "edu.artic.audio"
        const val DETAILS: String = "edu.artic.details"
        const val AUDIO_DETAILS: String = "edu.artic.media.audio_details"
        const val AUDIO_TUTORIAL: String = "edu.artic.media.audio_tutorial"
        const val INFO: String = "edu.artic.info"
        const val INFO_MEMBER_CARD: String = "edu.artic.info/accessMemberCard"
        const val ARG_SEARCH_OBJECT: String = "ARG_SEARCH_OBJECT"
        const val ARG_EXHIBITION_OBJECT: String = "ARG_EXHIBITION_OBJECT"
        const val ARG_TOUR = "ARG_TOUR"
        const val ARG_TOUR_START_STOP = "ARG_TOUR_START_STOP"
        const val ARG_AMENITY_TYPE: String = "ARG_AMENITY_TYPE"
        const val ARG_AUDIO_TUTORIAL_RESULT: String = "ARG_AUDIO_TUTORIAL_RESULT"
    }
}

fun linkHome(): Intent {
    val intent = NavigationConstants.HOME.asDeepLinkIntent()
    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
    return intent
}