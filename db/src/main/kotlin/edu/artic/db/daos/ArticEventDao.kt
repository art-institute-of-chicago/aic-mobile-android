package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticDataObject
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import io.reactivex.Flowable

@Dao
interface ArticEventDao {

    @Query("DELETE FROM ArticEvent")
    fun clearTable()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateEvents(exhibitions: List<ArticEvent>)
}