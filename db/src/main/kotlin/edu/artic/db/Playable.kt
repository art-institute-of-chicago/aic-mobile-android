package edu.artic.db

/**
 * Interface for a database model that [edu.artic.media.audio.AudioPlayerService]
 * can play.
 *
 * Either contains or references at least one
 * [audio track][edu.artic.db.models.AudioFileModel].
 *
 * @author Sameer Dhakal (Fuzz)
 */
interface Playable {
    /**
     * Retrieve the address of the preferred image to display next to the
     * audio controls while this media is playing. Expected to be a network
     * url (`https://`), but there is room to expand to file urls in future.
     *
     * May be null in the unlikely case that no such uri was provided by the API.
     *
     * **Implementation note:** the returned Uri has already been passed through
     * [String.asCDNUri][edu.artic.ui.util.asCDNUri].
     */
    fun getPlayableThumbnailUrl(): String?

    /**
     * A friendly title for this media, to display next to the audio controls.
     *
     * While null values are not strictly supported, the app has no reason to
     * crash if that field is missing. Nulls should thus be converted to empty
     * Strings (or a sane placeholder) before use.
     */
    fun getPlayableTitle(): String?
}