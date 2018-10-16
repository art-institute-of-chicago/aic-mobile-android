package edu.artic.db

import android.content.Context
import com.fuzz.rx.bindTo
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticDataObject
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter

/**
 * Implementation of [AppDataServiceProvider] that derives [ArticAppData] from local files.
 *
 * It will always return empty lists when asked for [events][getEvents] or
 * [exhibitions][getExhibitions].
 */
class LocalAppDataServiceProvider(
        private val context: Context,
        dataObjectDao: ArticDataObjectDao
) : AppDataServiceProvider {

    private val dataObject: Subject<ArticDataObject> = BehaviorSubject.create()

    /**
     * The returned object can parse and print timestamps in accordance with
     * [https://tools.ietf.org/html/rfc7232#section-2.2].
     *
     * Sample format: `Tue, 15 Nov 1994 12:45:26 GMT`
     */
    private val rfc7232Formatter: DateTimeFormatter
        get() {
            // TODO: use correct format
            return DateTimeFormatter.ISO_DATE_TIME
        }

    init {
        dataObjectDao
                .getDataObject()
                .bindTo(dataObject)
    }

    override fun getBlobHeaders(): Observable<Map<String, List<String>>> {
        val museumZone = ZoneId.of("America/Chicago")

        val lastModified: ZonedDateTime = ZonedDateTime.of(
                LocalDate.now(museumZone),
                LocalTime.MIN,
                museumZone
        )

        return Observable.just(
                mapOf(
                        "last_modified" to listOf(rfc7232Formatter.format(lastModified))
                )
        )
    }

    override fun getBlob(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            context.assets.open("app-data-v2.json").use {

                // TODO: Read in from JSON file

                return@create
            }
        }
    }

    override fun getExhibitions(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            dataObject.subscribeBy(
                    onError = { observer.onError(it) },
                    onNext = { _ ->
                        // TODO: Return empty result
                        return@subscribeBy
                    })
        }
    }

    override fun getEvents(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            dataObject.subscribeBy(
                    onError = { observer.onError(it) },
                    onNext = { _ ->
                        // TODO: Return empty result
                        return@subscribeBy
                    })
        }
    }

}
