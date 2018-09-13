package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import edu.artic.ui.util.asCDNUri
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticSearchArtworkObject(
        val artworkId: String = "",
        val audioObject: ArticObject? = null,
        val title: String,
        val thumbnailUrl: String?,
        val imageUrl: String?,
        val artistDisplay: String?,
        val location: String?,
        val floor: Int,
        val galleryId: String?

) : Parcelable {


    /**
     * Alias for [thumbnailFullPath], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val thumbUrl: String?
        get() {
            return thumbnailUrl?.asCDNUri()
        }



    /**
     * Alias for [largeImageFullPath], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val largeImageUrl: String?
        get() {
            return imageUrl?.asCDNUri()
        }
}
