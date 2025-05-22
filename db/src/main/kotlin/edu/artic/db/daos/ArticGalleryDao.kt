package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.artic.db.models.ArticGallery
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface ArticGalleryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addGalleries(galleries: List<ArticGallery>)

    @Query("select * from ArticGallery where galleryId = :id")
    fun getGalleryForGalleryIdSynchronously(id: String): ArticGallery?

    @Query("select * from ArticGallery where title = :title LIMIT 1")
    fun getGalleryByTitle(title: String): Single<ArticGallery>

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

    @Query("delete from ArticGallery")
    fun clear()
}