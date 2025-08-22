package edu.artic.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.artic.db.daos.*
import edu.artic.db.models.*

@Database(
        entities = [
            DashBoard::class,
            ArticGeneralInfo::class,
            ArticGallery::class,
            ArticObject::class,
            ArticAudioFile::class,
            ArticTour::class,
            ArticMapAnnotation::class,
            ArticExhibition::class,
            ArticExhibitionCMS::class,
            ArticEvent::class,
            ArticDataObject::class,
            ArticMapFloor::class,
            ArticSearchSuggestionsObject::class,
            ArticMessage::class
        ],
        version = 13,
        exportSchema = true
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val dashboardDao: DashboardDao
    abstract val generalInfoDao: GeneralInfoDao
    abstract val galleryDao: ArticGalleryDao
    abstract val objectDao: ArticObjectDao
    abstract val audioFileDao: ArticAudioFileDao
    abstract val tourDao: ArticTourDao
    abstract val exhibitionCMSDao: ArticExhibitionCMSDao
    abstract val mapAnnotationDao: ArticMapAnnotationDao
    abstract val dataObjectDao: ArticDataObjectDao
    abstract val exhibitionDao: ArticExhibitionDao
    abstract val eventDao: ArticEventDao
    abstract val articMapFloorDao: ArticMapFloorDao
    abstract val searchSuggestionDao: ArticSearchObjectDao
    abstract val messageDao: ArticMessageDao
}
