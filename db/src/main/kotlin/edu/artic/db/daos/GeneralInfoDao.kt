package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticGeneralInfo
import io.reactivex.Flowable

@Dao
interface GeneralInfoDao {
    @Query("select * from ArticGeneralInfo limit 1")
    fun getGeneralInfo(): Flowable<ArticGeneralInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setGeneralInfo(generalInfo: ArticGeneralInfo): Long

    /**
     * For sanity checks in [edu.artic.db.AppDataManager.enforceSanityCheck].
     *
     * Must return 0 if there's no data, 1 if there _is_ data,
     * any other number means something is wrong.
     */
    @Query("select count(*) from ArticGeneralInfo limit 1")
    fun getRowCount(): Int
}