package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticMapFloor
import io.reactivex.Flowable

@Dao
interface ArticMapFloorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMapFloors(list: List<ArticMapFloor>)

    @Query("select * from ArticMapFloor")
    fun getMapFloors() : Flowable<List<ArticMapFloor>>
}