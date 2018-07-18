package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticGeneralInfo
import edu.artic.db.models.DashBoard

@Dao
interface GeneralInfoDao {
    @Query("select * from ArticGeneralInfo limit 1")
    fun getGeneralInfo() : ArticGeneralInfo

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setGeneralInfo(generalInfo: ArticGeneralInfo): Long
}