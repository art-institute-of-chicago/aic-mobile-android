package edu.artic.db

import android.content.Context
import com.fuzz.rx.bindTo
import com.squareup.moshi.Moshi
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticDataObject
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.progress.ProgressEventBus
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import okio.Okio
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.ChronoField
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Locale

/**
 * Implementation of [AppDataServiceProvider] that derives [ArticAppData] from local files.
 *
 * It will always return empty lists when asked for [events][getEvents] or
 * [exhibitions][getExhibitions].
 */
class LocalAppDataServiceProvider(
        private val moshi: Moshi,
        private val context: Context,
        dataObjectDao: ArticDataObjectDao
) : AppDataServiceProvider {

    private val dataObject: Subject<ArticDataObject> = BehaviorSubject.create()

    private val zeroResults = ResultPagination(0, 0, 0, 0, 0)


    /**
     * The returned object can parse and print timestamps in accordance with
     * [https://tools.ietf.org/html/rfc7232#section-2.2].
     *
     * Sample format: `Tue, 15 Nov 1994 12:45:26 GMT`
     */
    private val rfc7232Formatter: DateTimeFormatter
        get() {
            return DateTimeFormatterBuilder()
                    .appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT)
                    .appendLiteral(", ")
                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
                    .appendLiteral(' ')
                    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
                    .appendLiteral(' ')
                    .appendValue(ChronoField.YEAR, 4)
                    .appendLiteral(' ')
                    .append(DateTimeFormatter.ISO_LOCAL_TIME)
                    .appendLiteral(' ')
                    .appendZoneText(TextStyle.SHORT)
                    .toFormatter(Locale.ENGLISH)
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

            val stream: InputStream = try {
                context.assets.open("app-data-v2.json")
            } catch (fnf: FileNotFoundException) {
                observer.onError(NoAppDataException(fnf))
                return@create
            }

            stream.use {

                val data = moshi.adapter(ArticAppData::class.java).fromJson(Okio.buffer(Okio.source(it)))

                if (data != null) {
                    observer.onNext(
                            ProgressDataState.Done(data)
                    )
                    observer.onComplete()
                } else {
                    observer.onError(NoAppDataException())
                }

                return@create
            }
        }
    }

    override fun getExhibitions(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            dataObject.subscribeBy(
                    onError = { observer.onError(it) },
                    onNext = { _ ->
                        observer.onNext(doneWithZeroResults<ArticExhibition>())
                        observer.onComplete()
                        return@subscribeBy
                    })
        }
    }

    override fun getEvents(): Observable<ProgressDataState> {
        return Observable.create { observer ->
            dataObject.subscribeBy(
                    onError = { observer.onError(it) },
                    onNext = { _ ->
                        observer.onNext(doneWithZeroResults<ArticEvent>())
                        observer.onComplete()
                        return@subscribeBy
                    })
        }
    }

    class NoAppDataException(cause: Throwable? = null): NullPointerException(
            "Unfortunately, we can't figure out where the application data is stored." +
            "\nDouble-check the 'How To Build' section of the project README."
    ) {

        init {
            initCause(cause)
        }
    }

    /**
     * Use this to indicate the end of the current [progress stream][ProgressEventBus] sequence.
     *
     * See also the `:splash` module's `edu.artic.splash.SplashViewModel`.
     */
    private fun <T> doneWithZeroResults(): ProgressDataState.Done<ArticResult<T>> {
        return ProgressDataState.Done(
                ArticResult(
                        zeroResults,
                        emptyList()
                )
        )
    }
}
