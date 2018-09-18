package edu.artic.info

import android.arch.lifecycle.ViewModel
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.db.ApiModule
import edu.artic.db.constructRetrofit
import edu.artic.viewmodel.ViewModelKey
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.Executor
import javax.inject.Named
import javax.inject.Singleton

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class InfoModule {


    @Binds
    @IntoMap
    @ViewModelKey(InformationViewModel::class)
    abstract fun informationViewModel(informationViewModel: InformationViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val informationFragment: InformationFragment

    @Binds
    @IntoMap
    @ViewModelKey(MuseumInformationViewModel::class)
    abstract fun museumInformationViewModel(museumInformationViewModel: MuseumInformationViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val museumInformationFragment: MuseumInformationFragment

    @get:ContributesAndroidInjector
    abstract val infoActivity: InfoActivity


    @Module
    companion object {

        @JvmStatic
        @Provides
        fun provideMemberInfoPreferencesManager(context: Context): MemberInfoPreferencesManager = MemberInfoPreferencesManager(context)


        @JvmStatic
        @Provides
        @Singleton
        @Named(MEMBER_INFO_API)
        fun provideRetrofit(baseUrl: String,
                            @Named(ApiModule.BLOB_CLIENT_API)
                            client: OkHttpClient,
                            executor: Executor,
                            moshi: Moshi): Retrofit =
                constructRetrofit(baseUrl = BuildConfig.MEMBER_INFO_API, client = client, executor = executor, moshi = moshi, xml = true)

        @JvmStatic
        @Provides
        @Singleton
        fun provideMemberInfoService(@Named(MEMBER_INFO_API) retrofit: Retrofit) = RetrofitMemberDataProvider(retrofit)

        const val MEMBER_INFO_API = "MEMBER_INFO_API"
    }

}
