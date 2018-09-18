package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.Executor

/**
 * Utility function for creating the precise instance of [Retrofit] that
 * we desire for executing [AppDataManager.loadData]. It ensures the
 * following capabilities:
 *
 * * Network calls are executed by a specific [OkHttpClient]
 *     * this defines certain timeouts for the calls
 *     * in debug builds it also provides helpful logging settings
 * * Network calls are scheduled in an RxJava-compatible way
 *     * this lets us use Rx types in our Retrofit definition files
 *     * we make particular use of [io.reactivex.Observable] and [io.reactivex.Flowable]
 *     * more details available in docs for [RxJava2CallAdapterFactory]
 * * Network responses are converted into Kotlin or Java types
 *     * simple stuff is converted into java primitives and [java.lang.String]s
 *     * complex json-like structures are converted into Kotlin objects by [MoshiConverterFactory]
 *     * parsing is done [leniently][com.squareup.moshi.JsonReader.setLenient]
 *
 * Additional customization can be performed by passing a custom extension
 * function in as [customConfiguration].
 */
inline fun constructRetrofit(
        baseUrl: String,
        client: OkHttpClient,
        executor: Executor,
        moshi: Moshi,
        xml: Boolean = false,
        schedulerFactory: CallAdapter.Factory = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()),
        customConfiguration: Retrofit.Builder.() -> Unit = {}): Retrofit {

    val builder = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .callbackExecutor(executor)
            .addCallAdapterFactory(schedulerFactory)
            .addConverterFactory(ScalarsConverterFactory.create())
            .apply(customConfiguration)

    if (!xml) {
        builder.addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
    } else {
        builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict(
                Persister(AnnotationStrategy())
        ))
    }

    return builder.build()

}
