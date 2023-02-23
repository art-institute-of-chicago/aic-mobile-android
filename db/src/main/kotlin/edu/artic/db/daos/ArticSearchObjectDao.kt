package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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