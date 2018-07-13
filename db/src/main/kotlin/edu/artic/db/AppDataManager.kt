package edu.artic.db

import io.reactivex.Observable
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatterBuilder
import javax.inject.Inject

class AppDataManager @Inject constructor(
        private val serviceProvider: AppDataServiceProvider,
        private val appDatabase: AppDatabase
) {

    fun getBlob(): Observable<AppDataState> {
        return serviceProvider.getBlobHeaders()
                .flatMap {
                    val dtf = DateTimeFormatterBuilder()
                            //Thu, 12 Jul 2018 16:56:12 GMT
                            .appendPattern("E, dd MMM yyyy HH:mm:ss O")
                            .toFormatter()
                    if (it.containsKey("last-modified")) {
                        val lastModified = it["last-modified"]?.get(0)!!
                        val newLocalDateTime = LocalDateTime.parse(lastModified, dtf)
                        if (newLocalDateTime.isAfter(/*TODO: get from storage*/ LocalDateTime.parse("Thu, 12 Jul 2018 16:56:12 GMT", dtf))) {
                            return@flatMap serviceProvider.getBlob()
                        } else {
                            return@flatMap Observable.just(AppDataState.Empty)
                        }
                    } else {
                        serviceProvider.getBlob()
                    }

                }.doOnNext {
                    if (it is AppDataState.Done) {
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


                    }
                }
    }
}