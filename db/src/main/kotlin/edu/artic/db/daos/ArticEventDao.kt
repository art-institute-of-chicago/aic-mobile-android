package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticEvent
import io.reactivex.Flowable

@Dao
interface ArticEventDao {

    @Query("DELETE FROM ArticEvent")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateEvents(exhibitions: List<ArticEvent>)

    @Query("select * from ArticEvent order by title limit 4")
    fun getEventSummary(): Flowable<List<ArticEvent>>
}