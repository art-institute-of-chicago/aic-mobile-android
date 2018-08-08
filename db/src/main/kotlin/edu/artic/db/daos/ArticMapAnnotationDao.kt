package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticMapAnnotation
import edu.artic.db.models.ArticMapAnnotationType
import io.reactivex.Flowable

@Dao
abstract class ArticMapAnnotationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAnnotations(tours: List<ArticMapAnnotation>)

    @Query("select * from ArticMapAnnotation where annotationType = :type")
    abstract fun getAnnotationByType(type: String): Flowable<List<ArticMapAnnotation>>

    @Query("select * from ArticMapAnnotation where annotationType = :type and floor = :floor")
    abstract fun getAnnotationByTypeForFloor(type: String, floor: String): Flowable<List<ArticMapAnnotation>>

    @Query("select * from ArticMapAnnotation where annotationType = \"Text\" and textType = :type ")
    abstract fun getTextAnnotationByType(type: String): Flowable<List<ArticMapAnnotation>>

    @Query("select * from ArticMapAnnotation where annotationType = \"Text\" and textType = :type and floor = :floor")
    abstract fun getTextAnnotationByTypeAndFloor(type: String, floor: String): Flowable<List<ArticMapAnnotation>>

    @Query("select * from ArticMapAnnotation where annotationType = :annotationType and textType = :type ")
    abstract fun getTextAnnotationByType(type: String, annotationType: String = ArticMapAnnotationType.TEXT): Flowable<List<ArticMapAnnotation>>

    fun getBuildingNamesOnMap(): Flowable<List<ArticMapAnnotation>> {
        return getAnnotationByType("Text")
    }

    fun getAmenitiesOnMapForFloor(floor: String): Flowable<List<ArticMapAnnotation>> {
        return getAnnotationByTypeForFloor("Amenity", floor)
    }

    fun getDepartmentOnMapForFloor(floor: String): Flowable<List<ArticMapAnnotation>> {
        return getAnnotationByTypeForFloor(ArticMapAnnotationType.DEPARTMENT, floor)
    }
}