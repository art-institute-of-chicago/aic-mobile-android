package edu.artic.db

import io.reactivex.Observable
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatterBuilder
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

                    val dtf = DateTimeFormatterBuilder()
                            //Thu, 12 Jul 2018 16:56:12 GMT
                            .appendPattern("E, dd MMM yyyy HH:mm:ss O")
                            .toFormatter()
                    if (headers.containsKey(HEADER_LAST_MODIFIED)) {
                        val lastModified = headers[HEADER_LAST_MODIFIED]?.get(0)!!
                        val newLocalDateTime = LocalDateTime.parse(lastModified, dtf)
                        val storedLastModified = appDataPreferencesManager.lastModified
                        if (storedLastModified.isNotEmpty() && newLocalDateTime.isAfter(LocalDateTime.parse(storedLastModified, dtf))) {
                            return@flatMap serviceProvider.getBlob()
                        } else {
                            return@flatMap Observable.just(AppDataState.Empty)
                        }
                    } else {
                        serviceProvider.getBlob()
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