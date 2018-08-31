package edu.artic.db

import com.fuzz.rx.asObservable
import edu.artic.db.daos.*
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticSearchSuggestionsObject
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import javax.inject.Inject
import javax.inject.Singleton

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
        private val searchSuggestionDao: ArticSearchObjectDao
) {
    companion object {
        const val HEADER_LAST_MODIFIED = "last-modified"
        private const val PRIMARY_DOWNLOADED_PERCENT = .34f
        //Currently calculated because we are downloading only 3 endpoints
        private const val PER_OBJECT_PERCENT = .33f
        private const val MAX_SECONDARY_DOWNLOADS = 2
    }


    /**
     * @return an observable that will show how close to completion we are and a Done if fully complete
     */
    fun loadData(): Observable<ProgressDataState> {
        return Observable.create<ProgressDataState> { observer ->
            //First load app data, once app data is successfully loaded
            getBlob().subscribe({ appDataState ->
                if (appDataState is ProgressDataState.Done<*> || appDataState === ProgressDataState.Empty) {
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

    fun getBlob(): Observable<ProgressDataState> {
        return serviceProvider.getBlobHeaders()
                .flatMap { headers ->
                    if (!headers.containsKey(HEADER_LAST_MODIFIED) || headers[HEADER_LAST_MODIFIED]?.get(0)
                            != appDataPreferencesManager.lastModified) {
                        serviceProvider.getBlob()
                    } else {
                        ProgressDataState.Empty.asObservable()
                    }
                }.flatMap { appDataState ->
                    if (appDataState is ProgressDataState.Done<*>) {
                        //Save last downloaded headers
                        appDataState.headers[HEADER_LAST_MODIFIED]?.let {
                            appDataPreferencesManager.lastModified = it[0]
                        }

                        // runs the whole operation in a transaction.
                        appDatabase.runInTransaction {
                            val result = appDataState.result as ArticAppData

                            dashboardDao.setDashBoard(result.dashboard)
                            generalInfoDao.setGeneralInfo(result.generalInfo)

                            val galleries = result.galleries?.values?.toList()
                            if (galleries?.isNotEmpty() == true) {
                                galleryDao.addGalleries(galleries)
                            }

                            val audioFiles = result.audioFiles
                            if (audioFiles?.isNotEmpty() == true) {
                                audioFileDao.addAudioFiles(audioFiles.values.toList())
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
                                objectDao.addObjects(objects.values.toList())
                            }

                            val tours = result.tours
                            if (tours?.isNotEmpty() == true) {
                                tourDao.clear()
                                tours.forEach { tour ->
                                    // assign the first stop's floor to tour if tour's floor is invalid
                                    if (tour.floorAsInt == INVALID_FLOOR && tour.tourStops.isNotEmpty()) {
                                        tour.tourStops.first().objectId?.let { firstTourStopId ->
                                            tour.floor = objects?.get(firstTourStopId)?.floor ?: INVALID_FLOOR
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

                            dataObjectDao.setDataObject(result.data)

                            result.search?.let { searchObject ->
                                val searchKeywordSuggestions = searchObject.searchStrings.values.toList()
                                val artworkSuggestions = searchObject.searchObjects.map { it -> it.toString() }
                                searchSuggestionDao.setDataObject(ArticSearchSuggestionsObject(searchKeywordSuggestions, artworkSuggestions))
                            }
                        }

                    }
                    return@flatMap appDataState.asObservable()
                }
    }

    private fun loadSecondaryData(): Observable<Int> {
        return Observable.zip(
                getExhibitions(),
                getEvents(),
                BiFunction<ProgressDataState, ProgressDataState, Int> { exhibitions, events ->
                    var currentDownloads = 0
                    if (exhibitions is ProgressDataState.Done<*>) {
                        currentDownloads++
                    }
                    if (events is ProgressDataState.Done<*>) {
                        currentDownloads++
                    }
                    return@BiFunction currentDownloads
                })

    }

    private fun getExhibitions(): Observable<ProgressDataState> {
        return serviceProvider.getExhibitions()
                .flatMap { progressDataState ->
                    when (progressDataState) {
                        is ProgressDataState.Done<*> -> {
                            val result = progressDataState.result as ArticResult<*>
                            if (result.data.isNotEmpty() && result.data[0] is ArticExhibition) {
                                exhibitionDao.clear()
                                /**
                                 * Update the sort order of the exhibitions according to the ArticExhibitionCMS
                                 */
                                exhibitionCMSDao
                                        .getAllCMSExhibitions()
                                        .subscribe { cmsExhibitionList ->
                                            @Suppress("UNCHECKED_CAST")
                                            val list = (result as ArticResult<ArticExhibition>).data
                                            val mapExhibitionByID = list.associateBy { it.id.toString() }
                                            cmsExhibitionList.forEach { exhibitionCMS ->
                                                mapExhibitionByID[exhibitionCMS.id]?.order = exhibitionCMS.sort
                                                // Override with exhibitions optional images from CMS, if available
                                                exhibitionCMS.imageUrl?.let {
                                                    mapExhibitionByID[exhibitionCMS.id]?.legacy_image_mobile_url = it
                                                }
                                                mapExhibitionByID[exhibitionCMS.id]?.order = exhibitionCMS.sort
                                            }
                                            exhibitionDao.updateExhibitions(list)
                                        }
                            }
                        }
                    }

                    progressDataState.asObservable()
                }
    }


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