package edu.artic.db.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticAppData(
        @Json(name = "dashboard") val dashboard: DashBoard?,
        @Json(name = "general_info") val generalInfo: ArticGeneralInfo,
        @Json(name = "galleries") val galleries: Map<String, ArticGallery?>?,
        @Json(name = "objects") val objects: Map<String, ArticObject?>?,
        @Json(name = "audio_files") val audioFiles: Map<String, ArticAudioFile?>?,
        @Json(name = "tours") val tours: List<ArticTour?>?,
        @Json(name = "map_annontations") val mapAnnotations: Map<String, ArticMapAnnotation?>?,
        @Json(name = "map_floors") val mapFloors: Map<String, ArticMapFloor>,
        @Json(name = "tour_categories") val tourCategories: Map<String, ArticTourCategory?>?,
        @Json(name = "exhibitions") val exhibitions: List<ArticExhibitionCMS?>?,
        @Json(name = "data") val data: ArticDataObject,
        @Json(name = "search") val search: ArticSearchObject?
)

@Entity
@JsonClass(generateAdapter = true)
data class DashBoard(
        @PrimaryKey
        val id: Int = 0,
        @Json(name = "featured_tours") val featuredTours: List<String>,
        @Json(name = "featured_exhibitions") val featuredExhibitions: List<String>
)

