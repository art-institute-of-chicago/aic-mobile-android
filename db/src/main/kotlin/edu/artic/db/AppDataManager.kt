package edu.artic.db

import android.util.Log
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
    }

    fun loadData(): Observable<ProgressDataState> {

        //First load app data. if empty is returned, do nothing, otherwise download everything else.
        return Observable.create<ProgressDataState> { observer ->

            getBlob().subscribe {
                when (it) {

                    is ProgressDataState.Done<*> -> {
                        loadPostAppData()
                                .subscribe({ postAppData ->
                                    when (postAppData) {
                                        is ProgressDataState.Downloading -> {
                                            Log.d("AppdataManager", "${postAppData.progress}")
                                            if (postAppData.progress >= 1) {
                                                observer.onNext(ProgressDataState.Downloading(1.0f))
                                                observer.onNext(ProgressDataState.Done(it.result))
                                            } else {
                                                observer.onNext(
                                                        ProgressDataState.Downloading(.33f + (postAppData.progress * .67f)))
                                            }
                                        }
                                    }
                                }, {
                                    observer.onError(it)
                                }, {
                                    observer.onComplete()
                                })
                    }
                    is ProgressDataState.Empty -> {
                        loadPostAppData()
                                .subscribe({ postAppData ->
                                    when (postAppData) {
                                        is ProgressDataState.Downloading -> {
                                            Log.d("AppdataManager", "${postAppData.progress}")
                                            if (postAppData.progress >= 1) {
                                                observer.onNext(ProgressDataState.Downloading(1.0f))
                                                observer.onNext(ProgressDataState.Empty)
                                            } else {
                                                observer.onNext(
                                                        ProgressDataState.Downloading(postAppData.progress))
                                            }
                                        }
                                    }
                                }, {
                                    observer.onError(it)
                                }, {
                                    observer.onComplete()
                                })
                    }
                    is ProgressDataState.Downloading -> {
                        observer.onNext(ProgressDataState.Downloading(it.progress * .33f))
                    }
                }

            }
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

    private fun loadPostAppData(): Observable<ProgressDataState> {
        return Observable.combineLatest(
                getExhibitions()
                        .filter { it is ProgressDataState.Downloading }
                        .map { it as ProgressDataState.Downloading },
                getEvents()
                        .filter { it is ProgressDataState.Downloading }
                        .map { it as ProgressDataState.Downloading },
                BiFunction<ProgressDataState.Downloading, ProgressDataState.Downloading, ProgressDataState> { exhibitionProgress, eventsProgress ->
                    Log.d("LoadPostAppData", "exhibitionProgress: ${exhibitionProgress.progress} eventProgress ${eventsProgress.progress}")
                    ProgressDataState.Downloading(
                            (exhibitionProgress.progress + eventsProgress.progress) / 2.0f
                    )
                })

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