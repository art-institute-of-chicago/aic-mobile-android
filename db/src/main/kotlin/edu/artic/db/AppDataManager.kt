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
                .flatMap {

                    val dtf = DateTimeFormatterBuilder()
                            //Thu, 12 Jul 2018 16:56:12 GMT
                            .appendPattern("E, dd MMM yyyy HH:mm:ss O")
                            .toFormatter()
                    if (it.containsKey(HEADER_LAST_MODIFIED)) {
                        val lastModified = it[HEADER_LAST_MODIFIED]?.get(0)!!
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

                }.flatMap {
                    if (it is AppDataState.Done) {
                        //Save last downloaded headers
                        it.headers[HEADER_LAST_MODIFIED]?.let {
                            appDataPreferencesManager.lastModified = it[0]
                        }

                        try {
                            appDatabase.dashboardDao.setDashBoard(it.result.dashboard)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        try {
                            appDatabase.generalInfoDao.setGeneralInfo(it.result.generalInfo)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        if (it.result.galleries?.isNotEmpty() == true) {
                            try {
                                appDatabase.galleryDao.addGalleries(it.result.galleries.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }

                        if (it.result.objects?.isNotEmpty() == true) {
                            try {
                                appDatabase.objectDao.addObjects(it.result.objects.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }

                        if (it.result.audioFiles?.isNotEmpty() == true) {
                            try {
                                appDatabase.audioFileDao.addAudioFiles(it.result.audioFiles.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }

                        if (it.result.tours?.isNotEmpty() == true) {
                            try {
                                appDatabase.articTourDao.addTours(it.result.tours)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                        if (it.result.mapAnnotations?.isNotEmpty() == true) {
                            try {
                                appDatabase.articMapAnnotationDao.addAnnotations(it.result.mapAnnotations.values.toList())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                    return@flatMap Observable.just(it)
                }
    }
}