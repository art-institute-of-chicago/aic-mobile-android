package edu.artic.db

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.jobinlawrance.downloadprogressinterceptor.DownloadProgressInterceptor
import com.jobinlawrance.downloadprogressinterceptor.ProgressEventBus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import edu.artic.db.daos.*
import edu.artic.membership.MemberDataProvider
import edu.artic.membership.MemberInfoPreferencesManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class ApiModule {

    @get:Multibinds
    @get:AdapterFactory
    abstract val jsonAdapterFactorySet: MutableSet<JsonAdapter.Factory>

    @Module
    companion object {

        private const val DEFAULT_TIMEOUT = 10L

        @JvmStatic
        @Provides
        @Singleton
        fun provideAppDataManager(
                serviceProvider: AppDataServiceProvider,
                appDataPreferencesManager: AppDataPreferencesManager,
                appDatabase: AppDatabase,
                dashboardDao: DashboardDao,
                generalInfoDao: GeneralInfoDao,
                audioFileDao: ArticAudioFileDao,
                galleryDao: ArticGalleryDao,
                tourDao: ArticTourDao,
                exhibitionCMSDao: ArticExhibitionCMSDao,
                mapAnnotationDao: ArticMapAnnotationDao,
                dataObjectDao: ArticDataObjectDao,
                eventDao: ArticEventDao,
                exhibitionDao: ArticExhibitionDao,
                objectDao: ArticObjectDao,
                articMapFloorDao: ArticMapFloorDao,
                searchSuggestionDao: ArticSearchObjectDao,
                messageDao: ArticMessageDao,
                appDataPrefManager: AppDataPreferencesManager,
                memberDataProvider: MemberDataProvider,
                memberInfoPreferencesManager: MemberInfoPreferencesManager
        ): AppDataManager = AppDataManager(
                serviceProvider,
                appDataPreferencesManager,
                appDatabase,
                dashboardDao,
                generalInfoDao,
                audioFileDao,
                galleryDao,
                tourDao,
                exhibitionCMSDao,
                mapAnnotationDao,
                dataObjectDao,
                eventDao,
                exhibitionDao,
                objectDao,
                articMapFloorDao,
                searchSuggestionDao,
                messageDao,
                appDataPrefManager,
                memberDataProvider,
                memberInfoPreferencesManager
        )

        @JvmStatic
        @Provides
        @Singleton
        fun provideRetrofitExecutor(): Executor = MainThreadExecutor()

        @JvmStatic
        @Provides
        @Singleton
        fun provideJsonAdapterFactory(@AdapterFactory creators: MutableSet<JsonAdapter.Factory>) =
                JsonAdapterFactory(creators)

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
                            moshi: Moshi
        ): Retrofit? {
            return if (baseUrl.contains("https")) {
                constructRetrofit(
                        baseUrl = baseUrl,
                        client = client,
                        executor = executor,
                        customConfiguration = {
                            addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
                        })
            } else {
                null
            }
        }

        @JvmStatic
        @Provides
        @Singleton
        @Named(BLOB_CLIENT_API)
        fun provideClient(progressEventBus: ProgressEventBus): OkHttpClient {
            val builder = OkHttpClient.Builder()
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor(DownloadProgressInterceptor(progressEventBus))
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }

            return builder.build()

        }

        @JvmStatic
        @Provides
        @Singleton
        fun provideProgressEventBus(): ProgressEventBus = ProgressEventBus()


        @JvmStatic
        @Provides
        @Singleton
        fun provideAppDataPreferencesManager(context: Context): AppDataPreferencesManager = AppDataPreferencesManager(context)

        @JvmStatic
        @Provides
        @Singleton
        fun provideBlobProvider(
                @Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit?,
                moshi: Moshi,
                context: Context,
                progressEventBus: ProgressEventBus,
                dataObjectDao: ArticDataObjectDao
        ): AppDataServiceProvider {
            return if (retrofit == null) {
                LocalAppDataServiceProvider("app-data-v3.json", moshi, context, dataObjectDao)
            } else {
                RetrofitAppDataServiceProvider(retrofit, progressEventBus, dataObjectDao)
            }
        }


        @JvmStatic
        @Provides
        @Singleton
        fun provideMoshi(jsonAdapterFactory: JsonAdapterFactory): Moshi = getMoshi { jsonAdapterFactory.applyTo(this) }

        const val RETROFIT_BLOB_API = "RETROFIT_BLOB_API"
        const val RETROFIT_BLOB_URL = "RETROFIT_BLOB_URL"
        const val BLOB_CLIENT_API = "BLOB_CLIENT_API"
    }

}

class MainThreadExecutor : Executor {
    private val handler = Handler(Looper.getMainLooper())

    override fun execute(r: Runnable) {
        handler.post(r)
    }
}
