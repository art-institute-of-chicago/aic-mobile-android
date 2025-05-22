package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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