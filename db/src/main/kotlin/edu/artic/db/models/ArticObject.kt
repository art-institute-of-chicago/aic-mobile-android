package edu.artic.db.models

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import edu.artic.db.Playable
import edu.artic.ui.util.asCDNUri
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Entity
@Parcelize
data class ArticObject(
        @Json(name = "title") val title: String = "",
        @Json(name = "status") val status: String? = null,
        @Json(name = "nid") @PrimaryKey val nid: String = "",
        @Json(name = "type") val type: String? = null,
        @Json(name = "id") val id: Int? = 0,
        @Json(name = "object_id") val objectId: Int? = 0,
        @Json(name = "alt_titles") val altTitles: String? = "",
        @Json(name = "main_reference_number") val mainReferenceNumber: String? = "",
        @Json(name = "boost_rank") val boostRank: String? = "",
        @Json(name = "date_display") val dateDisplay: String? = "",
        @Json(name = "artist_culture_place_delim") val artistCulturePlaceDelim: String? = "",
        @Json(name = "dimensions") val dimensions: String? = "",
        @Json(name = "artwork_type_title") val artworkTypeTitle: String? = "",
        @Json(name = "artwork_type_id") val artworkTypeId: String? = "",
        @Json(name = "credit_line") val creditLine: String? = "",
        @Json(name = "copyright_notice") val copyrightNotice: String? = "",
        @Json(name = "in_gallery") val inGallery: Boolean = false,
        @Json(name = "subject_id") val subjectId: String? = "",
        @Json(name = "technique_id") val techniqueId: String? = "",
        @Json(name = "color") val color: String? = "",
        @Json(name = "tour_titles") val tourTitles: String? = "",
        @Json(name = "location") val location: String? = "",
        @Json(name = "image_filename") val image_filename: String? = "",
        @Json(name = "image_url") val image_url: String? = "",
        @Json(name = "image_filemime") val imageFileMime: String? = "",
        @Json(name = "image_filesize") val imageFileSize: String? = "",
        @Json(name = "image_width") val imageWidth: String? = "",
        @Json(name = "image_height") val imageHeight: String? = "",
        @Json(name = "thumbnail_crop_v2") @Embedded(prefix = "thumbnail_crop_rect") val thumbnailCropRectV2: CropRect? = null,
        @Json(name = "thumbnail_full_path") val thumbnailFullPath: String? = null,
        @Json(name = "large_image_crop_v2") @Embedded(prefix = "large_image_crop_rect") val largeImageCropRectV2: CropRect? = null,
        @Json(name = "large_image_full_path") val largeImageFullPath: String? = null,
        @Json(name = "title_t") val titleT: String? = null,
        @Json(name = "gallery_location") val galleryLocation: String? = null,
        @Json(name = "reference_num") val referenceNum: String? = null,
        @Json(name = "audio_commentary") val audioCommentary: List<AudioCommentaryObject> = listOf(),
        @Json(name = "highlighted_object") val highlightedObject: String? = null,
        @Deprecated("Please do not use the 'full' image, as it is too large to fit on screen. Migrate to 'image_url' or 'large_image_full_path' immediately.")
        @Json(name = "full_image_full_path") val fullImageFullPath: String? = null,
//        @Json(name = "audio") val audio: List<String?>, Removed for now until json gets fixed as sometimes returns string othertimes object
        @Json(name = "audio_transcript") val audioTranscript: String? = null,
        @Json(name = "object_selector_number") val objectSelectorNumber: String? = null,
        @Json(name = "object_selector_numbers") val objectSelectorNumbers: List<String?> = listOf(),
        @Json(name = "is_on_view") var isOnView: Boolean? = true,
        // manually populated via DB save.
        var floor: Int = Int.MIN_VALUE
) : Parcelable, Playable {

    /**
     * Alias for [image_url], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val standardImageUrl: String?
        get() {
            return image_url?.asCDNUri()
        }

    /**
     * Alias for [largeImageFullPath], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val largeImageUrl: String?
        get() {
            return largeImageFullPath?.asCDNUri()
        }

    /**
     * Alias for [thumbnailFullPath], adjusted to the [CDN endpoint][String.asCDNUri] if appropriate.
     */
    val thumbUrl: String?
        get() {
            return thumbnailFullPath?.asCDNUri()
        }

    /**
     * Alias for just the artis
     */
    val tombstone: String?
    get() {
        return artistCulturePlaceDelim?.replace("|", "/r")
    }

    @Ignore
    constructor() : this(floor = Int.MIN_VALUE)

    /**
     * Delegates to [largeImageUrl] by default.
     *
     * See [Playable.getPlayableThumbnailUrl] for more info.
     */
    override fun getPlayableThumbnailUrl(): String? {
        return largeImageUrl
    }

    override fun getPlayableTitle(): String? {
        return this.title
    }
}

val ArticObject.audioFile: ArticAudioFile?
    get() = this.audioCommentary.firstOrNull()?.audioFile

