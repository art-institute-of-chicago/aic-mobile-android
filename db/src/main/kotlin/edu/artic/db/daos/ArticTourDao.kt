package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticTour

@Dao
interface ArticTourDao {

    @Query("select * from ArticTour limit 1")
    fun getFirstTour(): ArticTour

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addTours(tours: List<ArticTour>)
}