package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.support.annotation.WorkerThread
import edu.artic.db.models.ArticDataObject
import io.reactivex.Flowable

@Dao
interface ArticDataObjectDao {
    @Query("select * from ArticDataObject where id = 0")
    fun getDataObject(): Flowable<ArticDataObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setDataObject(generalInfo: ArticDataObject): Long

    /**
     * For sanity checks in [edu.artic.db.AppDataManager.enforceSanityCheck].
     *
     * Must return 0 if there's no data, 1 if there _is_ data,
     * any other number means something is wrong.
     */
    @WorkerThread
    @Query("select count(*) from ArticDataObject where id = 0")
    fun getRowCount(): Int
}