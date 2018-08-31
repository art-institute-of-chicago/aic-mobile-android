package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticMapFloor
import io.reactivex.Observable

/**
 * Description:
 */
@Dao
interface ArticMapFloorDao {

    @Query("SELECT * FROM ArticMapFloor where label = :number")
    fun floorByFloorNumber(number: Int): Observable<ArticMapFloor>
}