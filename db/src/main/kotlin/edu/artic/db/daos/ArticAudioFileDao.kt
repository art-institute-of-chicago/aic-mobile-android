package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.ArticAudioFile
import io.reactivex.Flowable

@Dao
interface ArticAudioFileDao {
    @Query("select * from ArticAudioFile limit 1")
    fun getFirstAudioFile(): ArticAudioFile

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAudioFiles(files: List<ArticAudioFile>)

    @Query("select * from ArticAudioFile where nid = :id")
    fun getAudioById(id: String): ArticAudioFile

    @Query("select * from ArticAudioFile where nid = :id")
    fun getAudioByIdAsync(id: String): Flowable<ArticAudioFile>

    @Query("delete from ArticAudioFile")
    fun clear()
}