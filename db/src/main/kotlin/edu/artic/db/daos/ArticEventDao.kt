package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticEvent
import io.reactivex.Flowable

@Dao
interface ArticEventDao {

    @Query("delete from ArticEvent")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateEvents(exhibitions: List<ArticEvent>)

    @Query("select * from ArticEvent where start_at > :earliestTime order by start_at limit 6 ")
    fun getEventSummary(earliestTime: Long = System.currentTimeMillis()): Flowable<List<ArticEvent>>

    @Query("select * from ArticEvent order by start_at, title")
    fun getAllEvents(): Flowable<List<ArticEvent>>
}