package edu.artic.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import edu.artic.db.daos.DashboardDao
import edu.artic.db.models.DashBoard

@Database(
        entities = [
            DashBoard::class],
        version = 1,
        exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val dashboardDao: DashboardDao
}