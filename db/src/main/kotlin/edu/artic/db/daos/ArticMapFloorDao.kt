package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.ArticMapFloor
import io.reactivex.Flowable

@Dao
interface ArticMapFloorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMapFloors(list: List<ArticMapFloor>)

    @Query("select * from ArticMapFloor")
    fun getMapFloors(): Flowable<List<ArticMapFloor>>
}