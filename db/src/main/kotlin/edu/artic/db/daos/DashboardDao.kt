package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.DashBoard

@Dao
interface DashboardDao {
    @Query("select * from DashBoard where id = 0")
    fun getCurrentDashboad(): DashBoard

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setDashBoard(dashBoard: DashBoard): Long
}