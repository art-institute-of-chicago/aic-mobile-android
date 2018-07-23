package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticExhibition
import io.reactivex.Flowable

@Dao
interface ArticExhibitionDao {

    @Query("delete from ArticExhibition")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateExhibitions(exhibitions: List<ArticExhibition>)

    @Query("select * from ArticExhibition")
    fun getAllExhibitions(): Flowable<List<ArticExhibition>>

    @Query("select * from ArticExhibition order by `order` limit 6")
    fun getExhibitionSummary(): Flowable<List<ArticExhibition>>

    @Query("select * from ArticExhibition where id = :id")
    fun getExhibitionById(id: Int) : Flowable<ArticExhibition>
}