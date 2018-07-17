package edu.artic.db

import com.fuzz.rx.asObservable
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import io.reactivex.Observable
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
            getBlob().subscribe({
                if (it is ProgressDataState.Done<*> || it === ProgressDataState.Empty) {
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
                } else if (it is ProgressDataState.Downloading) {
                    observer.onNext(ProgressDataState.Downloading(it.progress * PER_OBJECT_PERCENT))
                }

            }, {
                observer.onError(it)
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

                        try {
                            appDatabase.dashboardDao.setDashBoard(result.dashboard)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        try {
                            appDatabase.generalInfoDao.setGeneralInfo(result.generalInfo)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        val galleries = result.galleries
                        if (galleries?.isNotEmpty() == true) {
                            try {
                                appDatabase.galleryDao.addGalleries(galleries.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                        val objects = result.objects
                        if (objects?.isNotEmpty() == true) {
                            try {
                                appDatabase.objectDao.addObjects(objects.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                        val audioFiles = result.audioFiles
                        if (audioFiles?.isNotEmpty() == true) {
                            try {
                                appDatabase.audioFileDao.addAudioFiles(audioFiles.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }

                        val tours = result.tours
                        if (tours?.isNotEmpty() == true) {
                            try {
                                appDatabase.articTourDao.addTours(tours)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }

                        val mapAnnotations = result.mapAnnotations
                        if (mapAnnotations?.isNotEmpty() == true) {
                            try {
                                appDatabase.articMapAnnotationDao.addAnnotations(mapAnnotations.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }

                        try {
                            appDatabase.dataObjectDao.setDataObject(result.data)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    return@flatMap appDataState.asObservable()
                }
    }

    private fun loadSecondaryData(): Observable<Int> {
        return Observable.create { observer ->
            var currentDownloads = 0
            observer.onNext(currentDownloads)
            getExhibitions().subscribe({
                if (it is ProgressDataState.Done<*>) {
                    currentDownloads++
                    observer.onNext(currentDownloads)
                }
            }, {
                observer.onError(it)
            }, {
                if (currentDownloads == MAX_SECONDARY_DOWNLOADS) {
                    observer.onComplete()
                }
            })
            getEvents().subscribe({
                if (it is ProgressDataState.Done<*>) {
                    currentDownloads++
                    observer.onNext(currentDownloads)
                }
            }, {
                observer.onError(it)
            }, {
                if (currentDownloads == MAX_SECONDARY_DOWNLOADS) {
                    observer.onComplete()
                }
            })
        }

    }

    private fun getExhibitions(): Observable<ProgressDataState> {
        return serviceProvider.getExhibitions()
                .flatMap {
                    when (it) {
                        is ProgressDataState.Done<*> -> {
                            val result = it.result as ArticResult<*>
                            if (result.data.isNotEmpty() && result.data[0] is ArticExhibition) {
                                @Suppress("UNCHECKED_CAST")
                                appDatabase.exhibitionDao.updateExhibitions((result as ArticResult<ArticExhibition>).data)

                            }
                        }
                    }

                    it.asObservable()
                }
    }


    private fun getEvents(): Observable<ProgressDataState> {
        return serviceProvider.getEvents()
                .flatMap {
                    when (it) {
                        is ProgressDataState.Done<*> -> {
                            val result = it.result as ArticResult<*>
                            if (result.data.isNotEmpty() && result.data[0] is ArticEvent) {
                                @Suppress("UNCHECKED_CAST")
                                appDatabase.eventDao.updateEvents((result as ArticResult<ArticEvent>).data)

                            }
                        }
                    }

                    it.asObservable()
                }
    }
}