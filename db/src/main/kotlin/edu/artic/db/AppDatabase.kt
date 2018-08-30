package edu.artic.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
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
            ArticDataObject::class
        ],
        version = 2,
        exportSchema = false
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
    abstract val exhibitionDao : ArticExhibitionDao
    abstract val eventDao: ArticEventDao
}