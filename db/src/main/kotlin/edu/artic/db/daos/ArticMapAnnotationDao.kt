package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticMapAnnotation
import io.reactivex.Flowable

@Dao
abstract class ArticMapAnnotationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAnnotations(tours: List<ArticMapAnnotation>)

    @Query("select * from ArticMapAnnotation where annotationType = :type")
    abstract fun getAnnotationByType(type: String): Flowable<List<ArticMapAnnotation>>

    @Query("select * from ArticMapAnnotation where annotationType = \"Text\" and textType = :type")
    abstract fun getTextAnnotationByType(type: String): Flowable<List<ArticMapAnnotation>>

    fun getBuildingNamesOnMap(): Flowable<List<ArticMapAnnotation>> {
        return getTextAnnotationByType("Space")
    }
    fun getAmenitiesOnMap() : Flowable<List<ArticMapAnnotation>> {
        return getAnnotationByType("Amenity")
    }

    fun getDepartmentOnMap() : Flowable<List<ArticMapAnnotation>> {
        return getAnnotationByType("Department")
    }
}