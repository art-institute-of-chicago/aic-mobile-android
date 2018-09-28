package edu.artic

import artic.edu.localization.ui.LocalizationUiModule
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import edu.artic.accesscard.AccessMemberModule
import edu.artic.analytics.AnalyticsModule
import edu.artic.audio.AudioModule
import edu.artic.db.ApiModule
import edu.artic.db.DBModule
import edu.artic.details.DetailsModule
import edu.artic.events.EventsModule
import edu.artic.exhibitions.ExhibitionsModule
import edu.artic.info.InfoModule
import edu.artic.localization.LocalizerModule
import edu.artic.location.LocationModule
import edu.artic.location.LocationUIModule
import edu.artic.map.MapModule
import edu.artic.media.audio.MediaModule
import edu.artic.media.ui.MediaUiModule
import edu.artic.membership.MembershipModule
import edu.artic.search.SearchModule
import edu.artic.splash.SplashModule
import edu.artic.tours.ToursModule
import edu.artic.viewmodel.ViewModelModule
import edu.artic.welcome.WelcomeModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    LocalizerModule::class,
    SplashModule::class,
    WelcomeModule::class,
    ToursModule::class,
    ExhibitionsModule::class,
    EventsModule::class,
    ViewModelModule::class,
    ApiModule::class,
    DBModule::class,
    InfoModule::class,
    MapModule::class,
    AudioModule::class,
    AnalyticsModule::class,
    AndroidSupportInjectionModule::class,
    MediaUiModule::class,
    MediaModule::class,
    SearchModule::class,
    DetailsModule::class,
    LocationModule::class,
    LocationUIModule::class,
    LocalizationUiModule::class,
    MembershipModule::class,
    AccessMemberModule::class
])
interface AppComponent : AndroidInjector<ArticApp> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<ArticApp>() {

        abstract override fun build(): AppComponent
    }
}