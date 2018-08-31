package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticSearchSuggestionsObject
import io.reactivex.Flowable

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Dao
interface ArticSearchObjectDao {

    @Query("select * from ArticSearchSuggestionsObject where id = 0")
    fun getDataObject(): Flowable<ArticSearchSuggestionsObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setDataObject(searchObject: ArticSearchSuggestionsObject): Long
}