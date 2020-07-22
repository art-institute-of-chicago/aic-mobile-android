package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
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
