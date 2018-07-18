package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticExhibitionCMS
import io.reactivex.Flowable

@Dao
interface ArticExhibitionCMSDao {

    @Query("DELETE FROM ArticExhibitionCMS")
    fun clear()

    @Query("select * from ArticExhibitionCMS")
    fun getAllCMSExhibitions(): Flowable<List<ArticExhibitionCMS>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addCMSExhibitions(exhibitions: List<ArticExhibitionCMS>)
}