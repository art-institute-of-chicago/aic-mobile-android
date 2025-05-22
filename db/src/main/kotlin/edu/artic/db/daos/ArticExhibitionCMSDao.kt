package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.ArticExhibitionCMS
import io.reactivex.Flowable

@Dao
interface ArticExhibitionCMSDao {

    @Query("delete from ArticExhibitionCMS")
    fun clear()

    @Query("select * from ArticExhibitionCMS")
    fun getAllCMSExhibitions(): Flowable<List<ArticExhibitionCMS>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addCMSExhibitions(exhibitions: List<ArticExhibitionCMS>)
}