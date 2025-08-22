package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.ArticEvent
import io.reactivex.Flowable
import org.threeten.bp.ZonedDateTime

@Dao
interface ArticEventDao {

    @Query("delete from ArticEvent")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateEvents(exhibitions: List<ArticEvent>)

    @Query("select * from ArticEvent where start_at > :earliestTime order by start_at limit 6 ")
    fun getEventSummary(earliestTime: ZonedDateTime = ZonedDateTime.now()): Flowable<List<ArticEvent>>

    @Query("select * from ArticEvent order by start_at, title")
    fun getAllEvents(): Flowable<List<ArticEvent>>
}