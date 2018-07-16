package edu.artic.db

import com.fuzz.rx.asObservable
import io.reactivex.Observable
import javax.inject.Inject

class AppDataManager @Inject constructor(
        private val serviceProvider: AppDataServiceProvider,
        private val appDataPreferencesManager: AppDataPreferencesManager,
        private val appDatabase: AppDatabase
) {
    companion object {
        const val HEADER_LAST_MODIFIED = "last-modified"
    }

    fun getBlob(): Observable<AppDataState> {
        return serviceProvider.getBlobHeaders()
                .flatMap { headers ->
                    if (!headers.containsKey(HEADER_LAST_MODIFIED) || headers[HEADER_LAST_MODIFIED]?.get(0)
                            != appDataPreferencesManager.lastModified) {
                        serviceProvider.getBlob()
                    } else {
                        AppDataState.Empty.asObservable()
                    }
                }.flatMap { appDataState ->
                    if (appDataState is AppDataState.Done) {
                        //Save last downloaded headers
                        appDataState.headers[HEADER_LAST_MODIFIED]?.let {
                            appDataPreferencesManager.lastModified = it[0]
                        }

                        val result = appDataState.result

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
                    }
                    return@flatMap Observable.just(appDataState)
                }
    }
}