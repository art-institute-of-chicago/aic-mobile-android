package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.ArticExhibition
import io.reactivex.Flowable

@Dao
interface ArticExhibitionDao {

    @Query("delete from ArticExhibition")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateExhibitions(exhibitions: List<ArticExhibition>)

    @Query("select * from ArticExhibition order by `order`")
    fun getAllExhibitions(): Flowable<List<ArticExhibition>>

    @Query("select * from ArticExhibition order by `order` limit 6")
    fun getExhibitionSummary(): Flowable<List<ArticExhibition>>

    @Query("select * from ArticExhibition where id = :id")
    fun getExhibitionById(id: Int): Flowable<ArticExhibition>
}