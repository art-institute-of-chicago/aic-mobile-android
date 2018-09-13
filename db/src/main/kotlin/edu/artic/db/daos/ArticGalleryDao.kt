package edu.artic.db.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import edu.artic.db.models.ArticGallery
import io.reactivex.Flowable

@Dao
interface ArticGalleryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addGalleries(galleries: List<ArticGallery>)

    /**
     * Retrieve all galleries with an id in the given list.
     *
     * Even if [requestedIds] has duplicates, the response will only contain
     * at most one of the found galleries.
     */
    @Query("select * from ArticGallery where galleryId in (:requestedIds)")
    fun getGalleriesForIdList(requestedIds: List<String>): List<ArticGallery>

    @Query("select * from ArticGallery where floor = :floor")
    fun getGalleriesForFloor(floor: String): Flowable<List<ArticGallery>>

    @Query("select * from ArticGallery where title = :name")
    fun getGalleryByNameOrNull(name: String?): ArticGallery?
}