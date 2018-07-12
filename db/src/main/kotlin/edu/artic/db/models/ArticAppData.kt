package edu.artic.db.models

import com.squareup.moshi.Json

data class ArticBlobData(
        @Json(name = "dashboard") val dashboard: DashBoard,
        @Json(name = "general_info ") val generalInfo: ArticGeneralInfo,
        @Json(name = "galleries") val galleries: Map<String, ArticGallery>,
        @Json(name = "objects") val objects: Map<String, ArticObject>,
        @Json(name = "audio_files") val audioFiles: Map<String, ArticAudioFile>,
        @Json(name = "tours") val tours: List<ArticTour>,
        @Json(name = "map_annontations") val mapAnnotations: Map<String, ArticMapAnnotation>,
        @Json(name = "map_floors ") val mapFloors: Map<String, ArticMapFloor>,
        @Json(name = "tour_categories ") val tourCategories: Map<String, ArticTourCategory>,
        @Json(name = "exhibitions") val exhibitions: List<ArticExhibition>,
        @Json(name = "data") val data: ArticDataObject,
        @Json(name = "search") val search: ArticSearchObject
)

data class DashBoard(
        @Json(name = "featured_tours") val featuredTours: List<String>,
        @Json(name = "featured_exhibitions") val featuredExhibitions: List<String>
)

