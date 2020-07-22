package edu.artic.message

import android.content.Context
import edu.artic.base.BasePreferencesManager
import edu.artic.db.models.ArticMessage

/**
 * Preferences related to the AIC messages seen by the user.
 */
class MessagePreferencesManager(context: Context) : BasePreferencesManager(context, "messagePreferences") {

    fun getSeenMessageNids() =
            getStringSet(SEEN_MESSAGE_NIDS_KEY) ?: setOf()

    fun markMessagesAsSeen(messages: List<ArticMessage>) {
        val seenMessageNids = getStringSet(SEEN_MESSAGE_NIDS_KEY) ?: setOf()
        putStringSet(SEEN_MESSAGE_NIDS_KEY, seenMessageNids.union(messages.map { it.nid }))
    }

    companion object {
        private const val SEEN_MESSAGE_NIDS_KEY = "seen_message_nids"
    }

}
