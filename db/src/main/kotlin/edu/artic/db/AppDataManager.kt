package edu.artic.db

import android.util.Log
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.DashboardDao
import edu.artic.db.daos.GeneralInfoDao
import io.reactivex.Observable
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatterBuilder
import javax.inject.Inject

class AppDataManager @Inject constructor(
        private val serviceProvider: AppDataServiceProvider,
        private val dashboardDao: DashboardDao,
        private val generalInfoDao: GeneralInfoDao,
        private val galleryDao: ArticGalleryDao) {

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
                            dashboardDao.setDashBoard(it.result.dashboard)
                        } catch (e : Throwable) {
                            e.printStackTrace()
                        }
                        try {
                            generalInfoDao.setGeneralInfo(it.result.generalInfo)
                        } catch (e : Throwable) {
                            e.printStackTrace()
                        }
                        if(it.result.galleries?.isNotEmpty() == true){
                            try {
                                galleryDao.addGalleries(it.result.galleries.values.toList())
                            } catch (e : Throwable) {
                                e.printStackTrace()
                            }
                        }


                    }
                }
    }
}