package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticMapAnnotation

@Dao
interface ArticMapAnnotationDao {

    @Query("select * from ArticMapAnnotation limit 1")
    fun getFirstAnnotation(): ArticMapAnnotation

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAnnotations(tours: List<ArticMapAnnotation>)
}