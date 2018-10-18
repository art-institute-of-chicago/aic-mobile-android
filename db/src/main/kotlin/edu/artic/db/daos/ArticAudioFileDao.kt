package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
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
}