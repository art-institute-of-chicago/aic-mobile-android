package edu.artic.membership

import android.arch.lifecycle.ViewModel
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.db.ApiModule
import edu.artic.db.constructRetrofit
import edu.artic.viewmodel.ViewModelKey
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

    @Binds
    @IntoMap
    @ViewModelKey(AccessMemberCardViewModel::class)
    abstract fun accessMemberCardViewModel(accessMemberCardViewModel: AccessMemberCardViewModel):
            ViewModel

    @get:ContributesAndroidInjector
    abstract val accessMemberCardFragment: AccessMemberCardFragment

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
                @Named(ApiModule.BLOB_CLIENT_API)
                client: OkHttpClient,
                executor: Executor
        ): Retrofit = constructRetrofit(
                baseUrl = BuildConfig.MEMBER_INFO_API,
                client = client,
                executor = executor,
                customConfiguration = {
                    addConverterFactory(SimpleXmlConverterFactory.createNonStrict(
                            Persister(AnnotationStrategy())
                    ))
                })

        @JvmStatic
        @Provides
        @Singleton
        fun provideMemberInfoService(
                @Named(MEMBER_INFO_API) retrofit: Retrofit
        ) = RetrofitMemberDataProvider(retrofit)

        const val MEMBER_INFO_API = "MEMBER_INFO_API"
    }

}