package edu.artic.info

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

    @Binds
    @IntoMap
    @ViewModelKey(LanguageSettingsViewModel::class)
    abstract fun languageSettingsViewModel(accessMemberCardViewModel: LanguageSettingsViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val languageSettingsFragment: LanguageSettingsFragment

    @get:ContributesAndroidInjector
    abstract val infoActivity: InfoActivity


}
