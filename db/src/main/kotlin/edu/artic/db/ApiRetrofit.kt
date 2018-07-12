package edu.artic.db

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.Executor

/**
 * Description:
 */
inline fun constructRetrofit(
        baseUrl: String,
        client: OkHttpClient,
        executor: Executor,
        moshi: Moshi,
        schedulerFactory: CallAdapter.Factory = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()),
        customConfiguration: Retrofit.Builder.() -> Unit = {}): Retrofit =
        Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .callbackExecutor(executor)
                .addCallAdapterFactory(schedulerFactory)
                .addConverterFactory(UnitConverterFactory)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
                .apply(customConfiguration)
                .build()