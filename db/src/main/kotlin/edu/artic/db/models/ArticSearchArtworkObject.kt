package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import edu.artic.ui.util.asCDNUri
import kotlinx.android.parcel.Parcelize

/**
 * Every ArticSearchArtworkObject is created off of a representative
 * [ArticObject].
 *
 * In the Swift codebase that field is called `audioObject`, while here
 * we refer to it as [backingObject].
 */
@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticSearchArtworkObject(
        val artworkId: String = "",
        /**
         * Use this to access the associated audio commentaries, if any are present.
         *
         * See [ArticObject.audioFile] for details.
         */
        val backingObject: ArticObject? = null,
        val title: String,
        val thumbnailUrl: String?,
        val imageUrl: String?,
        val artistTitle: String?,
        val artistDisplay: String?,
        val location: String?,
        val floor: Int,
        val gallery: ArticGallery?

) : Parcelable {

    /**
     * Alias for [thumbnailUrl], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val thumbUrl: String?
        get() {
            return thumbnailUrl?.asCDNUri()
        }



    /**
     * Alias for [imageUrl], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val largeImageUrl: String?
        get() {
            imageUrl?.let {
                return it.asCDNUri()
            }
            return thumbUrl
        }

    val locationValue: String
        get() {
            var retLocation = location
            gallery?.let {
                if (location.orEmpty().isEmpty()) {
                    retLocation = it.location
                }
            }
            return retLocation.orEmpty()
        }
}
