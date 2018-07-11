package edu.artic.db.models

data class BlobData(
        val dashboard : DashBoard,
        val general_info : ArticGeneralInfo,
        val galleries : Map<String, ArticGallery>,
        val objects : Map<String, ArticObject>,
        val audio_files : Map<String, ArticAudioFile>,
        val tours : List<ArticTour>,
        val map_annontations : Map<String, AricMapAnnotation>,
        val map_floors : Map<String, ArticMapFloor>,
        val tour_categories : Map<String, ArticTourCategory>,
        val exhibitions : List<ArticExhibition>,
        val data : ArticDataObject,
        val search : ArticSearchObject
)

data class DashBoard(
        val featured_tours : List<String>,
        val featured_exhibitions : List<String>
)

