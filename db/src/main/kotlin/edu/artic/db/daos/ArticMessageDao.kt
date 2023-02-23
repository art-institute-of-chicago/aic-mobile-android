package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.ArticMessage
import io.reactivex.Flowable

@Dao
interface ArticMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(list: List<ArticMessage>)

    @Query("select * from ArticMessage")
    fun getMessages(): Flowable<List<ArticMessage>>

    @Query("delete from ArticMessage")
    fun clear()

}
