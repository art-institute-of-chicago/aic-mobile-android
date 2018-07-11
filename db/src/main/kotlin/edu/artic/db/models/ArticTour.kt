package edu.artic.db.models

import com.squareup.moshi.Json

data class ArticTour(
val title : String,
val status : String,
val nid : String,
val type : String,
val translations : String,
val location : String,
val latitude : String,
val longitude : String,
val floor : String,
val image_filename : String,
val image_url : String,
val image_filemime : String,
val image_filesize : String,
val image_width : String,
val image_height : String,
//val thumbnail_crop_rect : [],
val thumbnail_full_path : String,
//val large_image_crop_rect : [],
val large_image_crop_v2 : CropRect,
val large_image_full_path : String,
val tour_banner : String ,
val selector_number : String ,
val description : String,
val description_html : String,
val tour_dates : TourDate,
val intro : String,
val intro_html : String,
val tour_duration : String,
val tour_audio : String,
val category : Any, // TODO: figure out what this is
val stops : List<Any>, //TODO: figure out what this is
val weight : Int,
val tour_stops: List<TourStop>
) {
    data class TourDate(
    val start_date : String,
    val end_date : String
    )

    data class TourStop(
        @Json(name = "object") val objectId: String,
        val audio_id : String,
        val audio_bumper : String,
        val sort : Int
        )
}