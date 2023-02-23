package edu.artic.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.annotation.WorkerThread
import edu.artic.db.models.ArticObject
import edu.artic.db.models.AudioCommentaryObject
import io.reactivex.Flowable

@Dao
interface ArticObjectDao {
    @Query("select * from ArticObject limit 1")
    fun getFirstObject(): ArticObject


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addObjects(objects: List<ArticObject>)

    //Must be Flowable due to need to use this in 2 places
    @Query("select * from ArticObject where nid = :id")
    fun getObjectById(id: String): Flowable<ArticObject>


    //This one is intended for use when we don't want to filter out null objects
    @Query("select * from ArticObject where id = :id")
    fun getObjectByIdSynchronously(id: String): ArticObject?

    /**
     * Returns 1 (or more) if the object exists in the database, 0 (or less) if
     * it's not there.
     */
    @Query("select count(*) from ArticObject where nid = :id limit 1")
    fun getObjectCountWithId(id: String): Int

    /**
     * Returns the [ArticObject] when the given [criteria] matches existing
     * [ArticObject.audioCommentary].
     */
    @Query("SELECT * from ArticObject where audioCommentary LIKE :criteria")
    fun getObjectForGivenAudioCommentaryCriteria(criteria: String): List<ArticObject>?

    /**
     * Retrieves all of the [ArticObject]s found in a specific
     * gallery. May be an empty list if none claim to belong to
     * the given gallery.
     *
     * @see ArticObject.galleryLocation
     * @see edu.artic.db.models.ArticGallery.floor
     */
    @Query("select * from ArticObject where galleryLocation in (:galleryTitle)")
    fun getObjectsInGallery(galleryTitle: String): List<ArticObject>

    /** Database does not preserve the order of ids.**/
    @Query(value = "select * from ArticObject where nid in (:ids) order by nid")
    fun getObjectsByIdList(ids: List<String>): Flowable<List<ArticObject>>

    @Query("select * from ArticObject where floor = :floor")
    fun getObjectsByFloor(floor: Int): Flowable<List<ArticObject>>

    @Query("delete from ArticObject")
    fun clear()
}


/**
 * Proxy to [ArticObjectDao.getObjectCountWithId]. Returns true if
 * we have an object in the database under that id, false otherwise.
 */
@WorkerThread
fun ArticObjectDao.hasObjectWithId(id: String?): Boolean {
    return id != null && getObjectCountWithId(id) > 0
}

/**
 * Looks up the object using [ArticObject.audioCommentary]'s
 * [AudioCommentaryObject.objectSelectorNumber].
 *
 * Not expected to be used much outside the 'audio' module.
 */
fun ArticObjectDao.getObjectBySelectorNumber(objectSelectorNumber: String): List<ArticObject>? {
    val likeStatement = """
                        %"object_selector_number":"$objectSelectorNumber"%
                    """.trimIndent()
    return getObjectForGivenAudioCommentaryCriteria(likeStatement)
}