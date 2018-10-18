package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.DashBoard

@Dao
interface DashboardDao {
    @Query("select * from DashBoard where id = 0")
    fun getCurrentDashboad() : DashBoard

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setDashBoard(dashBoard: DashBoard): Long
}