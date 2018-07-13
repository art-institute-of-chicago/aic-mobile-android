package edu.artic.db.models

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class ArticObject(
        @Json(name = "title") val title: String?,
        @Json(name = "status") val status: String?,
        @Json(name = "nid") val nid: String?,
        @Json(name = "type") val type: String?,
        @Json(name = "id") val id: Int?,
        @Json(name = "object_id") val objectId: Int?,
        @Json(name = "alt_titles") val altTitles: String?,
        @Json(name = "main_reference_number") val mainReferenceNumber: String?,
        @Json(name = "boost_rank") val boostRank: String?,
        @Json(name = "date_display") val dateDisplay: String?,
        @Json(name = "artist_culture_place_delim") val artistCulturePlaceDelim: String?,
        @Json(name = "dimensions") val dimensions: String?,
        @Json(name = "artwork_type_title") val artworkTypeTitle: String?,
        @Json(name = "artwork_type_id") val artworkTypeId: String?,
        @Json(name = "credit_line") val creditLine: String?,
        @Json(name = "copyright_notice") val copyrightNotice: String?,
        @Json(name = "in_gallery") val inGallery: Boolean,
        @Json(name = "subject_id") val subjectId: String?,
        @Json(name = "technique_id") val techniqueId: String?,
        @Json(name = "color") val color: String?,
        @Json(name = "tour_titles") val tourTitles: String?,
        @Json(name = "location") val location: String?,
        @Json(name = "image_filename") val image_filename: String?,
        @Json(name = "image_url") val image_url: String?,
        @Json(name = "image_filemime") val imageFileMime: String?,
        @Json(name = "image_filesize") val imageFileSize: String?,
        @Json(name = "image_width") val imageWidth: String?,
        @Json(name = "image_height") val imageHeight: String?,
        @Json(name = "thumbnail_crop_v2") @Embedded(prefix = "thumbnail_crop_rect") val thumbnailCropRectV2: CropRect?,
        @Json(name = "thumbnail_full_path") val thumbnailFullPath: String?,
        @Json(name = "large_image_crop_v2") @Embedded(prefix = "large_image_crop_rect") val largeImageCropRectV2: CropRect?,
        @Json(name = "large_image_full_path") val largeImageFullPath: String?,
        @Json(name = "title_t") val titleT: String?,
        @Json(name = "gallery_location") val galleryLocation: String?,
        @Json(name = "reference_num") val referenceNum: String?,
        @Json(name = "audio_commentary") val audioCommentary: List<AudioCommentaryObject>,
        @Json(name = "highlighted_object") val highlightedObject: String?,
        @Json(name = "full_image_full_path") val fullImageFullPath: String?,
//        @Json(name = "audio") val audio: List<String?>, Removed for now until json gets fixed as sometimes returns string othertimes object
        @Json(name = "audio_transcript") val audioTranscript: String?,
        @Json(name = "object_selector_number") val objectSelectorNumber: String?,
        @Json(name = "object_selector_numbers") val objectSelectorNumbers: List<String?>
) {

//    data class AudioObject(
//            @Json(name = "object_selector_number") val object_selector_number: String,
//            @Json(name = "audio") val audio: String
//    )
}