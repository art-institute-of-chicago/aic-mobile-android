package edu.artic.membership

import android.content.Context
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class MembershipModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        fun provideMemberInfoPreferencesManager(context: Context):
                MemberInfoPreferencesManager = MemberInfoPreferencesManager(context)


        @JvmStatic
        @Provides
        @Singleton
        @Named(MEMBER_INFO_API)
        fun provideRetrofit(
                @Named(MEMBER_CLIENT_API) client: OkHttpClient,
                executor: Executor
        ): Retrofit? {
            return if (BuildConfig.MEMBER_INFO_API.contains("https")) {
                Retrofit.Builder()
                        .baseUrl(BuildConfig.MEMBER_INFO_API)
                        .client(client)
                        .callbackExecutor(executor)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .addConverterFactory(
                                SimpleXmlConverterFactory.createNonStrict(
                                        Persister(AnnotationStrategy())
                                )
                        )
                        .build()
            } else {
                null
            }
        }

        @JvmStatic
        @Provides
        @Singleton
        @Named(MEMBER_CLIENT_API)
        fun provideClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }

            return builder.build()

        }

        @JvmStatic
        @Provides
        @Singleton
        fun provideMemberInfoService(@Named(MEMBER_INFO_API) retrofit: Retrofit?): MemberDataProvider {
            return if (retrofit == null) {
                NoContentMemberDataProvider
            } else {
                RetrofitMemberDataProvider(retrofit)
            }
        }

        private const val MEMBER_INFO_API = "MEMBER_INFO_API"
        private const val MEMBER_CLIENT_API = "MEMBER_CLIENT_API"
        private const val DEFAULT_TIMEOUT = 10L
    }

}