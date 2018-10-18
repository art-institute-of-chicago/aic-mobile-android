package edu.artic.membership

import android.content.Context
import dagger.Module
import dagger.Provides
import edu.artic.db.ApiModule
import edu.artic.db.constructRetrofit
import okhttp3.OkHttpClient
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.Executor
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
        fun provideRetrofit(@Named(ApiModule.BLOB_CLIENT_API)
                            client: OkHttpClient,
                            executor: Executor)
                : Retrofit? {
            return if (BuildConfig.MEMBER_INFO_API.contains("https")) {
                constructRetrofit(
                        baseUrl = BuildConfig.MEMBER_INFO_API,
                        client = client,
                        executor = executor,
                        customConfiguration = {
                            addConverterFactory(SimpleXmlConverterFactory.createNonStrict(
                                    Persister(AnnotationStrategy())
                            ))
                        })
            } else {
                null
            }
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

        const val MEMBER_INFO_API = "MEMBER_INFO_API"
    }

}