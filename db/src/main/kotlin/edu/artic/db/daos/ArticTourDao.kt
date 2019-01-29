package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticTour
import io.reactivex.Flowable

@Dao
interface ArticTourDao {

    @Query("select * from ArticTour limit 1")
    fun getFirstTour(): ArticTour

    @Query("select * from ArticTour limit 1")
    fun getAsyncFirstTour(): Flowable<ArticTour>

    @Query("select * from ArticTour where selectorNumber = :selectorNumber")
    fun getTourBySelectorNumber(selectorNumber: String): ArticTour?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addTours(tours: List<ArticTour>)

    @Query("select * from ArticTour limit 6")
    fun getTourSummary(): Flowable<List<ArticTour>>

    @Query("select * from ArticTour")
    fun getAllTours() : Flowable<List<ArticTour>>

    @Query("select * from ArticTour order by title")
    fun getTours() : Flowable<List<ArticTour>>

    @Query(value = "select * from ArticTour where nid in (:ids)")
    fun getToursByIdList(ids: List<String>): Flowable<List<ArticTour>>

    @Query("delete from ArticTour")
    fun clear()
}