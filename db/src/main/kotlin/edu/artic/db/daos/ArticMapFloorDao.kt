package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import edu.artic.db.models.ArticMapFloor

@Dao
interface ArticMapFloorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMapFloors(list: List<ArticMapFloor>)
}