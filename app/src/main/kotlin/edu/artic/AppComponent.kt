package edu.artic

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import edu.artic.db.ApiModule
import edu.artic.db.DBModule
import edu.artic.splash.SplashModule
import edu.artic.viewmodel.ViewModelModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    SplashModule::class,
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