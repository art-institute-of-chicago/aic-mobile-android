package edu.artic.db

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class ApiModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        @Named(RETROFIT_BLOB_URL)
        fun provideBaseUrl(): String = BuildConfig.BLOB_URL

        @JvmStatic
        @Provides
        @Singleton
        @Named(RETROFIT_BLOB_API)
        fun provideRetrofit(@Named(RETROFIT_BLOB_URL)
                            baseUrl: String,
                            @Named(BLOB_CLIENT_API)
                            client: OkHttpClient,
                            executor: Executor,
                            moshi: Moshi): Retrofit =
                constructRetrofit(baseUrl, client, executor, moshi)

        @JvmStatic
        @Provides
        @Singleton
        @Named(BLOB_CLIENT_API)
        fun provideClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(HttpLoggingInterceptor())
            }
            return builder.build()

        }

        @JvmStatic
        @Provides
        @Singleton
        fun provideBlobService(blobProvider: BlobProvider) = BlobService(blobProvider)

        @JvmStatic
        @Provides
        @Singleton
        fun provideBlobProvider(
                @Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit
        ): BlobProvider = RetrofitBlobProvider(retrofit)



        @JvmStatic
        @Provides
        @Singleton
        fun provideMoshi(jsonAdapterFactory: JsonAdapterFactory): Moshi = getMoshi { jsonAdapterFactory.applyTo(this) }

        const val RETROFIT_BLOB_API = "RETROFIT_BLOB_API"
        const val RETROFIT_BLOB_URL = "RETROFIT_BLOB_URL"
        const val BLOB_CLIENT_API = "BLOB_CLIENT_API"
    }

}