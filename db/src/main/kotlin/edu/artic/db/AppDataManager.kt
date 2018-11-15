package edu.artic.db

import android.support.annotation.WorkerThread
import com.fuzz.rx.asObservable
import edu.artic.db.daos.*
import edu.artic.db.models.*
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central singleton for the [ArticAppData] which underpins this app.
 *
 * This content should be refreshed regularly by invoking
 * [AppDataManager.loadData]. Note that the download may take some
 * time; see that method's docs for recommendations.
 *
 * Of special note: in stark contrast to the other DAOs injected here,
 * [ArticSearchObjectDao] and [ArticDataObjectDao] are expected
 * to contain at most one object apiece.
 */
@Singleton
class AppDataManager @Inject constructor(
        private val serviceProvider: AppDataServiceProvider,
        private val appDataPreferencesManager: AppDataPreferencesManager,
        private val appDatabase: AppDatabase,
        private val dashboardDao: DashboardDao,
        private val generalInfoDao: GeneralInfoDao,
        private val audioFileDao: ArticAudioFileDao,
        private val galleryDao: ArticGalleryDao,
        private val tourDao: ArticTourDao,
        private val exhibitionCMSDao: ArticExhibitionCMSDao,
        private val mapAnnotationDao: ArticMapAnnotationDao,
        private val dataObjectDao: ArticDataObjectDao,
        private val eventDao: ArticEventDao,
        private val exhibitionDao: ArticExhibitionDao,
        private val objectDao: ArticObjectDao,
        private val articMapFloorDao: ArticMapFloorDao,
        private val searchSuggestionDao: ArticSearchObjectDao,
        private val appDataPrefManager: AppDataPreferencesManager
) {
    companion object {
        const val HEADER_LAST_MODIFIED = "last-modified"
        private const val PRIMARY_DOWNLOADED_PERCENT = .34f
        //Currently calculated because we are downloading only 3 endpoints
        private const val PER_OBJECT_PERCENT = .33f
        private const val MAX_SECONDARY_DOWNLOADS = 2
    }


    /**
     * The returned observable emits multiple 'state's before completing.
     *
     * There is no 'start' emission. The first emission is a
     * [ProgressDataState.Downloading], and all subsequent progress events
     * are also emitted as [ProgressDataState.Downloading]s. Note that
     * the progress given is an aggregate of five separate tasks:
     *
     * 1. downloading the primary [app data][getBlob] from
     * [a designated url][BuildConfig.BLOB_URL]
     * 2. downloading the secondary ['exhibitions' data][getExhibitions]
     * 3. downloading the secondary ['events' data][getEvents]
     * 4. parsing all of the above-downloaded content into models
     * 5. saving said models to the [AppDatabase]
     *
     * Finally, the fully-parsed result is given inside a
     * [ProgressDataState.Done]. There is no explicit
     * [onComplete][io.reactivex.Observer.onComplete] event.
     *
     * See docs on [getBlob] for when to expect the
     * ['empty'][ProgressDataState.Empty] emission.
     *
     * @return an Observable that will show how close to completion we are
     */
    fun loadData(): Observable<ProgressDataState> {
        return Observable.create<ProgressDataState> { observer ->
            //First load app data, once app data is successfully loaded
            getBlob().subscribe({ appDataState ->
                if (appDataState is ProgressDataState.Done<*> || appDataState === ProgressDataState.Empty) {
                    appDataPrefManager.downloadedNecessaryData = true
                    loadSecondaryData()
                            .subscribe({ amountDownload ->
                                observer.onNext(
                                        ProgressDataState.Downloading(
                                                progress = PRIMARY_DOWNLOADED_PERCENT
                                                        + (amountDownload * PER_OBJECT_PERCENT))
                                )
                                if (amountDownload == MAX_SECONDARY_DOWNLOADS) {
                                    observer.onNext(ProgressDataState.Done(true))
                                }
                            }, {
                                observer.onError(it)
                            }, {
                                observer.onComplete()
                            })
                } else if (appDataState is ProgressDataState.Downloading) {
                    observer.onNext(ProgressDataState.Downloading(appDataState.progress * PER_OBJECT_PERCENT))
                }

            }, { appDataState ->
                observer.onError(appDataState)
            }, {
                //Don't care about on complete here
            })
        }
    }

    /**
     * Download the majority of an [ArticAppData] object from the
     * [designated url][BuildConfig.BLOB_URL].
     *
     * If the server's copy of this content has not changed since the
     * last query, our returned Observable will just emit a single
     * [ProgressDataState.Empty] and complete immediately.
     *
     * The actual download is executed by [serviceProvider], which
     * may be changed at injection time to e.g. a mock service. In
     * the production environment we expect it to be a
     * [RetrofitAppDataServiceProvider].
     */
    fun getBlob(): Observable<ProgressDataState> {
        return serviceProvider.getBlobHeaders()
                .observeOn(Schedulers.io())
                .flatMap { headers ->
                    // First, verify that we actually _have_ what we need. This is quick.
                    enforceSanityCheck()

                    // Next, see if the latest data is newer than what we have on file.
                    if (!headers.containsKey(HEADER_LAST_MODIFIED) || headers[HEADER_LAST_MODIFIED]?.get(0)
                            != appDataPreferencesManager.lastModified) {
                        serviceProvider.getBlob()
                    } else {
                        ProgressDataState.Empty.asObservable()
                    }
                }.flatMap { appDataState ->
                    if (appDataState is ProgressDataState.Done<*>) {

                        // runs the whole operation in a transaction.
                        appDatabase.runInTransaction {
                            val result = appDataState.result as ArticAppData
                            result.dashboard?.let { dashboard ->
                                dashboardDao.setDashBoard(dashboard)
                            }
                            generalInfoDao.setGeneralInfo(result.generalInfo)
                            result.mapFloors.values.filterNotNull().let { floors ->
                                articMapFloorDao.insertMapFloors(floors.toList())
                            }

                            val galleries : List<ArticGallery> = result.galleries?.values?.toList()?.filterNotNull().orEmpty()
                            if (galleries.isNotEmpty()) {
                                galleryDao.clear()
                                galleryDao.addGalleries(galleries)
                            }

                            val audioFiles = result.audioFiles
                            if (audioFiles?.isNotEmpty() == true) {
                                audioFileDao.clear()
                                audioFileDao.addAudioFiles(audioFiles.values.filterNotNull().toList())
                            }

                            val objects = result.objects
                            if (objects?.isNotEmpty() == true) {
                                objects.values.forEach { articObject ->
                                    // add a floor field to the object, since its not known in the JSON directly.
                                    galleries?.firstOrNull { it.title == articObject.galleryLocation }
                                            ?.let { gallery ->
                                                articObject.floor = gallery.floor
                                            }
                                    articObject.audioCommentary.forEach { audioCommentaryObject ->
                                        audioCommentaryObject.audio?.let {
                                            audioCommentaryObject.audioFile = audioFileDao.getAudioById(it)
                                        }
                                    }
                                }
                                objectDao.clear()
                                objectDao.addObjects(objects.values.toList())
                            }

                            val tours = result.tours?.filter { it.weight != null }
                            if (tours?.isNotEmpty() == true) {
                                tourDao.clear()
                                tours.forEach { tour ->
                                    // assign the first stop's floor to tour if tour's floor is invalid
                                    if (tour.tourStops.isNotEmpty()) {
                                        // Filter out stops without known objectIds (so-called 'ghost' stops)
                                        val iterator = tour.tourStops.iterator()
                                        while (iterator.hasNext()) {
                                            val tourStop = iterator.next()
                                            if (objectDao.hasObjectWithId(tourStop.objectId)) {
                                                continue
                                            } else {
                                                iterator.remove()
                                            }
                                        }
                                        // Make sure the tour itself has a floor number
                                        if (tour.floorAsInt == INVALID_FLOOR && tour.tourStops.isNotEmpty()) {
                                            tour.tourStops.first().objectId?.let { firstTourStopId ->
                                                tour.floor = objects?.get(firstTourStopId)?.floor ?: INVALID_FLOOR
                                            }
                                        }
                                    }

                                }
                                tourDao.addTours(tours)
                            }

                            val exhibitionsCMS = result.exhibitions
                            if (exhibitionsCMS?.isNotEmpty() == true) {
                                exhibitionCMSDao.clear()
                                exhibitionCMSDao.addCMSExhibitions(exhibitionsCMS)
                            }

                            val mapAnnotations = result.mapAnnotations
                            if (mapAnnotations?.isNotEmpty() == true) {
                                mapAnnotationDao.addAnnotations(mapAnnotations.values.toList())
                            }

                            ArticDataObject.IMAGE_SERVER_URL = result.data.imageServerUrl
                            dataObjectDao.setDataObject(result.data)

                            result.search?.let { searchObject ->
                                val searchKeywordSuggestions = searchObject.searchStrings.values.toList()
                                val artworkSuggestions = searchObject.searchObjects.map { it -> it.toString() }
                                searchSuggestionDao.setDataObject(ArticSearchSuggestionsObject(searchKeywordSuggestions, artworkSuggestions))
                            }


                            // Now that the transaction has reached its end successfully, we may
                            // save the last-modified date
                            appDataState.headers[HEADER_LAST_MODIFIED]?.let {
                                appDataPreferencesManager.lastModified = it[0]
                            }

                        }

                    }
                    return@flatMap appDataState.asObservable()
                }
    }

    /**
     * Internal mechanism for checking our database consistency. Right now we look
     * at two basic metrics:
     *
     * * is there exactly one [edu.artic.db.models.ArticGeneralInfo] in the db?
     * * is there exactly one [edu.artic.db.models.ArticDataObject] in the db?
     *
     * We chose these particular objects because they are particularly important
     * to the UI and any reconstruction mechanisms we might want to trigger.
     * If just one is missing, the app simply cannot operate in any great capacity
     * and we _must_ call [AppDataServiceProvider.getBlob].
     *
     * If either fails, it means that the database does not contain the
     * [basic ArticAppData][ArticAppData] content we need to function.
     */
    @WorkerThread
    private fun enforceSanityCheck() {
        // Make sure we're not bitten by https://issuetracker.google.com/issues/111504749
        // TODO: Remove this '::close' when we start using Room 2.1.0-a1 or higher
        if (!appDatabase.isOpen) {
            // This _should_ be a no-op, but in Room 2.0.0 and earlier it may release a stale database reference.
            appDatabase.openHelper.close()
            // If it did release a reference, the next line will throw with a helpful (though long) error message
        }

        if (generalInfoDao.getRowCount() != 1 || dataObjectDao.getRowCount() != 1) {
            // Absolutely no reason to keep previous data. Destroy it.
            appDataPreferencesManager.lastModified = ""
        }
    }

    /**
     * Retrieve the latest [exhibitions][getExhibitions] and [events][getEvents].
     *
     * Note that these are found at different API endpoints than the
     * [primary stuff][getBlob]. As a side-effect, the injected [serviceProvider]
     * (and not `this` manager) is responsible for determining precisely
     * how many exhibitions and events we parse when this method is called.
     *
     * Observers on this should implement `onError` to handle I/O errors. We
     * use [Observable.onErrorReturn] here to make sure that even if multiple
     * errors occur at a low-level, only one call would go out to that `onError`.
     */
    private fun loadSecondaryData(): Observable<Int> {
        return Observables.zip(
                getExhibitions()
                        .onErrorReturn {
                            ProgressDataState.Interrupted(it)
                        },
                getEvents()
                        .onErrorReturn {
                            ProgressDataState.Interrupted(it)
                        }
        ) { exhibitions, events ->

            // XXX: Instead of just throwing the first error we see, perhaps
            // we ought to figure out how to use rx's CompositeException

            var currentDownloads = 0
            when (exhibitions) {
                is ProgressDataState.Done<*> -> currentDownloads++
                is ProgressDataState.Interrupted -> throw exhibitions.error
            }
            when (events) {
                is ProgressDataState.Done<*> -> currentDownloads++
                is ProgressDataState.Interrupted -> throw events.error
            }

            return@zip currentDownloads
        }

    }

    /**
     * Retrieve a number of [exhibitions][ArticExhibition] from the API.
     *
     * Each retrieved exhibition will be augmented by a related
     * [ArticExhibitionCMS] already loaded by [getBlob]. Thus augmented,
     * these [ArticExhibition]s are finally saved directly
     * [into the dao][ArticExhibitionDao.updateExhibitions].
     *
     * In [the above query][RetrofitAppDataServiceProvider.getExhibitions]
     * the number of these exhibitions is limited to be at most 99. It is
     * possible that no exhibitions will be found; in that case whatever
     * is stored in [exhibitionDao] will be left alone.
     */
    private fun getExhibitions(): Observable<ProgressDataState> {
        return serviceProvider.getExhibitions()
                .flatMap { progress ->
                    when (progress) {
                        is ProgressDataState.Done<*> -> {
                            val result = progress.result as ArticResult<*>
                            val retrieved = result.data

                            if (retrieved.isNotEmpty() && retrieved[0] is ArticExhibition) {
                                exhibitionDao.clear()
                                /**
                                 * Update the sort order of the exhibitions according to the ArticExhibitionCMS
                                 */
                                exhibitionCMSDao
                                        .getAllCMSExhibitions()
                                        .subscribe { cmsExhibitionList ->
                                            @Suppress("UNCHECKED_CAST")
                                            val list = retrieved as List<ArticExhibition>

                                            val exhibitionsById = list.associateBy { it.id.toString() }

                                            cmsExhibitionList.forEach { exhibitionCMS: ArticExhibitionCMS ->
                                                exhibitionsById[exhibitionCMS.id]?.order = exhibitionCMS.sort
                                                // Override with exhibitions optional images from CMS, if available
                                                exhibitionCMS.imageUrl?.let {
                                                    exhibitionsById[exhibitionCMS.id]?.imageUrl = it
                                                }
                                                exhibitionsById[exhibitionCMS.id]?.order = exhibitionCMS.sort
                                            }


                                            val desiredIds = exhibitionsById.values.mapNotNull { it.gallery_id }
                                            val galleries : List<ArticGallery> = galleryDao.getGalleriesForIdList(desiredIds)

                                            val galleriesById: Map<String?, ArticGallery> = galleries.associateBy { it.galleryId }

                                            list.forEach { exhibition ->
                                                if (exhibition.gallery_id != null) {
                                                    val gallery = galleriesById[exhibition.gallery_id]
                                                    if (gallery?.location != null) {
                                                        exhibition.latitude = gallery.latitude
                                                        exhibition.longitude = gallery.longitude
                                                        exhibition.floor = gallery.floor
                                                    }
                                                }
                                            }

                                            exhibitionDao.updateExhibitions(list)
                                        }
                            }
                        }
                    }

                    progress.asObservable()
                }
    }


    /**
     * Retrieve a number of [events][ArticEvent] from the API.
     *
     * In [the above query][RetrofitAppDataServiceProvider.getEvents]
     * the number of these events is limited to be at most 500. It is
     * possible that no events will be found; in that case whatever
     * is stored in [eventDao] will be left alone.
     */
    private fun getEvents(): Observable<ProgressDataState> {
        return serviceProvider.getEvents()
                .flatMap { progressDataState ->
                    when (progressDataState) {
                        is ProgressDataState.Done<*> -> {
                            val result = progressDataState.result as ArticResult<*>
                            if (result.data.isNotEmpty() && result.data[0] is ArticEvent) {
                                eventDao.clear()
                                @Suppress("UNCHECKED_CAST")
                                eventDao.updateEvents((result as ArticResult<ArticEvent>).data)

                            }
                        }
                    }

                    progressDataState.asObservable()
                }
    }
}