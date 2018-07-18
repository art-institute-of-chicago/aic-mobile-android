package edu.artic

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import edu.artic.main.MainModule
import edu.artic.db.ApiModule
import edu.artic.db.DBModule
import edu.artic.splash.SplashModule
import edu.artic.viewmodel.ViewModelModule
import edu.artic.welcome.WelcomeModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    SplashModule::class,
    WelcomeModule::class,
    MainModule::class,
    ViewModelModule::class,
    ApiModule::class,
    DBModule::class,
    AndroidSupportInjectionModule::class
])
interface AppComponent : AndroidInjector<ArticApp> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<ArticApp>() {

        abstract override fun build(): AppComponent
    }
}