package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticGallery
import edu.artic.db.models.ArticGeneralInfo
import edu.artic.db.models.DashBoard

@Dao
interface ArticGalleryDao {
    @Query("select * from ArticGallery limit 1")
    fun getFirstGallery() : ArticGallery


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addGalleries(galleries: List<ArticGallery>)

}