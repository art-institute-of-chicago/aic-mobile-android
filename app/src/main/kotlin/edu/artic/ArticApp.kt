package edu.artic

import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DaggerApplication

class ArticApp : DaggerApplication() {


    override fun onCreate() {
        app = this
        super.onCreate()
        AndroidThreeTen.init(this)
    }
    override fun applicationInjector() = seedBuilder(DaggerAppComponent.builder())

    fun seedBuilder(builder: AppComponent.Builder): AppComponent {
        builder.seedInstance(this)
        val component = builder.build()
        ArticComponent.setInternalAppComponent(component)
        return component
    }


    companion object {

        @JvmStatic
        lateinit var app: ArticApp
            private set

    }
}