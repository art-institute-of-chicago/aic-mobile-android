package edu.artic

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import edu.artic.analytics.AnalyticsModule
import edu.artic.audio.AudioModule
import edu.artic.db.ApiModule
import edu.artic.db.DBModule
import edu.artic.events.EventsModule
import edu.artic.exhibitions.ExhibitionsModule
import edu.artic.info.InfoModule
import edu.artic.splash.SplashModule
import edu.artic.tours.ToursModule
import edu.artic.viewmodel.ViewModelModule
import edu.artic.welcome.WelcomeModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    SplashModule::class,
    WelcomeModule::class,
    ToursModule::class,
    ExhibitionsModule::class,
    EventsModule::class,
    ViewModelModule::class,
    ApiModule::class,
    DBModule::class,
    InfoModule::class,
    AudioModule::class,
    AnalyticsModule::class,
    AndroidSupportInjectionModule::class
])
interface AppComponent : AndroidInjector<ArticApp> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<ArticApp>() {

        abstract override fun build(): AppComponent
    }
}