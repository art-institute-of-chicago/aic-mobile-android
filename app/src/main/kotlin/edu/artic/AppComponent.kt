package edu.artic

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import edu.artic.splash.SplashModule
import edu.artic.viewmodel.ViewModelModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    SplashModule::class,
    ViewModelModule::class,
    AndroidSupportInjectionModule::class
])
interface AppComponent : AndroidInjector<ArticApp> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<ArticApp>() {

        abstract override fun build(): AppComponent
    }
}