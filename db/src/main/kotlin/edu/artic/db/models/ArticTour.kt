package edu.artic.db.models

import com.squareup.moshi.Json

data class ArticTour(
        @Json(name = "title") val title: String,
        @Json(name = "status") val status: String,
        @Json(name = "nid") val nid: String,
        @Json(name = "type") val type: String,
        @Json(name = "translations") val translations: List<Translation>,
        @Json(name = "location") val location: String,
        @Json(name = "latitude") val latitude: String,
        @Json(name = "longitude") val longitude: String,
        @Json(name = "floor") val floor: String,
        @Json(name = "image_filename") val imageFileName: String,
        @Json(name = "image_url") val imageUrl: String,
        @Json(name = "image_filemime") val imageFileMime: String,
        @Json(name = "image_filesize") val imageFileSize: String,
        @Json(name = "image_width") val imageWidth: String,
        @Json(name = "image_height") val imageHeight: String,
//val thumbnail_crop_rect : [], // Unsure what type goes here so leaving removed for now
        @Json(name = "thumbnail_full_path") val thumbnailFullPath: String,
//val large_image_crop_rect : [], // Unsure what type goes here so leaving removed for now
        @Json(name = "large_image_crop_v2") val largeImageCropV2: CropRect,
        @Json(name = "large_image_full_path") val largeImageFullPath: String,
        @Json(name = "tour_banner") val tourBanner: String,
        @Json(name = "selector_number") val selectorNumber: String,
        @Json(name = "description") val description: String,
        @Json(name = "description_html") val descriptionHtml: String,
        @Json(name = "tour_dates") val tourDates: TourDate,
        @Json(name = "intro") val intro: String,
        @Json(name = "intro_html") val introHtml: String,
        @Json(name = "tour_duration") val tourDuration: String,
        @Json(name = "tour_audio") val tourAudio: String,
        @Json(name = "category") val category: Any, // TODO: figure out what this is
        @Json(name = "stops") val stops: List<Any>, //TODO: figure out what this is
        @Json(name = "weight") val weight: Int,
        @Json(name = "tour_stops") val tourStops: List<TourStop>
) {
    data class TourDate(
            @Json(name = "start_date") val startDate: String,
            @Json(name = "end_date") val endDate: String
    )

    data class TourStop(
            @Json(name = "object") val objectId: String,
            val audio_id: String,
            val audio_bumper: String,
            val sort: Int
    )

    data class Translation(
            @Json(name = "language") val language: String,
            @Json(name = "title") val title: String,
            @Json(name = "description") val description: String,
            @Json(name = "description_html") val description_html: String,
            @Json(name = "intro") val intro: String,
            @Json(name = "intro_html") val intro_html: String,
            @Json(name = "tour_duration") val tour_duration: String
    )
}