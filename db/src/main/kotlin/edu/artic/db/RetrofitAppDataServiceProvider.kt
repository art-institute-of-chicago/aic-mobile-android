package edu.artic.db

import android.util.Log
import com.fuzz.retrofit.rx.requireValue
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.models.ArticDataObject
import edu.artic.db.progress.ProgressEventBus
import io.reactivex.Observable
import retrofit2.Retrofit
import javax.inject.Named

class RetrofitAppDataServiceProvider(
        @Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit,
        private val progressEventBus: ProgressEventBus,
        dataObjectDao: ArticDataObjectDao
) : AppDataServiceProvider {
    companion object {
        const val BLOB_HEADER_ID = "blob_download_header_id"
        const val EXHIBITIONS_HEADER_ID = "exhibitions_download_header_id"
        const val EVENT_HEADER_ID = "events_download_header_id"
    }

    init {
        dataObjectDao
                .getDataObject()
                .subscribe {
                    dataObject = it
                }

    }

    lateinit var dataObject: ArticDataObject

    private val service = retrofit.create(AppDataApi::class.java)

    override fun getBlob(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            val disposable = progressEventBus.observable()
                    .subscribe {
                        if (it.downloadIdentifier == BLOB_HEADER_ID) {
                            observer.onNext(ProgressDataState.Downloading(it.progress / 100f))
                        }
                    }
            service.getBlob(BLOB_HEADER_ID)
                    .subscribe({
                        if (it.isError) {
                            observer.onError(it.error())
                            it.error().printStackTrace()
                        } else {
                            observer.onNext(ProgressDataState.Done(it.requireValue(), it.response().headers().toMultimap()))
                        }
                        disposable.dispose()
                    }, {
                        observer.onError(it)
                        disposable.dispose()
                    }, {
                        observer.onComplete()
                        disposable.dispose()
                    })
        }
    }

    override fun getBlobHeaders(): Observable<Map<String, List<String>>> {
        return Observable.create { observer ->
            service.getBlobHeaders()
                    .subscribe({
                        observer.onNext(it.headers().toMultimap())
                    }, {
                        observer.onError(it)
                    }, {
                        observer.onComplete()
                    })

        }
    }

    override fun getExhibitions(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            var url = dataObject.dataApiUrl + dataObject.exhibitionsEndpoint
            if (!url.contains("/search")) {
                url += "/search"
            }
            url += "?limit=99"

            val postParams = mutableMapOf<String, Any>()
            postParams["fields"] = listOf(
                    "id",
                    "title",
                    "short_description",
                    "legacy_image_mobile_url",
                    "legacy_image_desktop_url",
                    "gallery_id",
                    "web_url",
                    "aic_start_at",
                    "aic_end_at"
            )
            postParams["sort"] = listOf("aic_start_at", "aic_end_at")
            postParams["query"] = mutableMapOf<String, Any>().apply {
                //Boolean map
                this["bool"] = mutableMapOf<String, Any>().apply {
                    this["must"] = mutableListOf<Any>().apply {
                        this.add(mutableMapOf<String, Any>().apply {
                            //range
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["aic_start_at"] = mutableMapOf<String, String>().apply {
                                    this["lte"] = "now"
                                }
                            }
                        })

                        this.add(mutableMapOf<String, Any>().apply {
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["aic_end_at"] = mutableMapOf<String, String>().apply {
                                    this["gte"] = "now"
                                }
                            }
                        })
                    }

                    this["must_not"] = mutableListOf<Any>().apply {
                        this.add(mutableMapOf<String, Any>().apply {
                            //range
                            this["term"] = mutableMapOf<String, Any>().apply {
                                this["status"] = "Closed"
                            }
                        })
                    }
                }
            }

            service.getExhibitions(EXHIBITIONS_HEADER_ID, url, postParams)
                    .subscribe({
                        if (!it.isError) {
                            observer.onNext(
                                    ProgressDataState.Done(
                                            it.requireValue(),
                                            it.response().headers().toMultimap()
                                    )
                            )
                        } else {
                            observer.onError(it.error())
                        }

                    }, {
                        observer.onError(it)

                    }, {
                        observer.onComplete()

                    }

                    )
        }
    }


    override fun getEvents(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            var url = dataObject.dataApiUrl + dataObject.eventsEndpoint
            if (!url.contains("/search")) {
                url += "/search"
            }
            url += "?limit=500"

            val postParams = mutableMapOf<String, Any>()
            postParams["fields"] = listOf(
                    "id",
                    "title",
                    "description",
                    "short_description",
                    "image",
                    "location",
                    "start_at",
                    "end_at",
                    "button_text",
                    "button_url"
            )
            postParams["sort"] = listOf("start_at", "end_at")
            postParams["query"] = mutableMapOf<String, Any>().apply {
                //Boolean map
                this["bool"] = mutableMapOf<String, Any>().apply {
                    this["must"] = mutableListOf<Any>().apply {
                        this.add(mutableMapOf<String, Any>().apply {
                            //range
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["start_at"] = mutableMapOf<String, String>().apply {
                                    this["lte"] = "now+2w"
                                }
                            }
                        })

                        this.add(mutableMapOf<String, Any>().apply {
                            this["range"] = mutableMapOf<String, Any>().apply {
                                this["end_at"] = mutableMapOf<String, String>().apply {
                                    this["gte"] = "now"
                                }
                            }
                        })
                    }
                }
            }

            service.getEvents(EVENT_HEADER_ID, url, postParams)
                    .subscribe({
                        if (!it.isError) {
                            observer.onNext(
                                    ProgressDataState.Done(
                                            it.requireValue(),
                                            it.response().headers().toMultimap()
                                    )
                            )
                        } else {
                            observer.onError(it.error())
                        }
                    }, {
                        observer.onError(it)
                    }, {
                        observer.onComplete()
                    }

                    )
        }
    }
}