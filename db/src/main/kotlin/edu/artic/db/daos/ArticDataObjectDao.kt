package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticDataObject
import io.reactivex.Flowable

@Dao
interface ArticDataObjectDao {
    @Query("select * from ArticDataObject where id = 0")
    fun getDataObject(): Flowable<ArticDataObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setDataObject(generalInfo: ArticDataObject): Long
}