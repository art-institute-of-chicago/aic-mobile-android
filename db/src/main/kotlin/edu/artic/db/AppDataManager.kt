package edu.artic.db

import com.fuzz.rx.asObservable
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import javax.inject.Inject

class AppDataManager @Inject constructor(
        private val serviceProvider: AppDataServiceProvider,
        private val appDataPreferencesManager: AppDataPreferencesManager,
        private val appDatabase: AppDatabase
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

                        val result = appDataState.result as ArticAppData

                        appDatabase.dashboardDao.setDashBoard(result.dashboard)
                        appDatabase.generalInfoDao.setGeneralInfo(result.generalInfo)

                        val galleries = result.galleries
                        if (galleries?.isNotEmpty() == true) {
                            appDatabase.galleryDao.addGalleries(galleries.values.toList())
                        }
                        val objects = result.objects
                        if (objects?.isNotEmpty() == true) {
                            appDatabase.objectDao.addObjects(objects.values.toList())
                        }
                        val audioFiles = result.audioFiles
                        if (audioFiles?.isNotEmpty() == true) {
                            appDatabase.audioFileDao.addAudioFiles(audioFiles.values.toList())
                        }

                        val tours = result.tours
                        if (tours?.isNotEmpty() == true) {
                            appDatabase.tourDao.clear()
                            appDatabase.tourDao.addTours(tours)
                        }

                        val exhibitionsCMS = result.exhibitions
                        if (exhibitionsCMS?.isNotEmpty() == true) {
                            appDatabase.exhibitionCMSDao.clear()
                            appDatabase.exhibitionCMSDao.addCMSExhibitions(exhibitionsCMS)
                        }

                        val mapAnnotations = result.mapAnnotations
                        if (mapAnnotations?.isNotEmpty() == true) {
                            appDatabase.mapAnnotationDao.addAnnotations(mapAnnotations.values.toList())
                        }

                        appDatabase.dataObjectDao.setDataObject(result.data)

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
                                appDatabase.exhibitionDao.clear()
                                /**
                                 * Update the sort order of the exhibitions according to the ArticExhibitionCMS
                                 */
                                appDatabase.exhibitionCMSDao
                                        .getAllCMSExhibitions()
                                        .subscribe { cmsExhibitionList ->
                                            @Suppress("UNCHECKED_CAST")
                                            val list = (result as ArticResult<ArticExhibition>).data
                                            val mapExhibitionByID = list.associateBy { it.id.toString() }
                                            cmsExhibitionList.forEach {
                                                mapExhibitionByID[it.id]?.order = it.sort
                                            }
                                            appDatabase.exhibitionDao.updateExhibitions(list)
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
                                appDatabase.eventDao.clear()
                                @Suppress("UNCHECKED_CAST")
                                appDatabase.eventDao.updateEvents((result as ArticResult<ArticEvent>).data)

                            }
                        }
                    }

                    progressDataState.asObservable()
                }
    }
}