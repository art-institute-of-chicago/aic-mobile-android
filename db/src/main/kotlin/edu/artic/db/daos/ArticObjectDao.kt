package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticObject
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface ArticObjectDao {
    @Query("select * from ArticObject limit 1")
    fun getFirstObject(): ArticObject


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addObjects(objects: List<ArticObject>)

    //Must be flowable due to need to use this in 2 places
    @Query("select * from ArticObject where nid = :id")
    fun getObjectById(id: String): Flowable<ArticObject>

    @Query("select * from ArticObject where objectSelectorNumber = :selectorNumber")
    fun getObjectBySelectorNumber(selectorNumber: String): Single<ArticObject>

}