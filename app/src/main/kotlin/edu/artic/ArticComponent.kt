package edu.artic

/**
 * Description: Holds onto our [AppComponent] for the app. We can swap out during testing.

 * @author Andrew Grosner (Fuzz)
 */

object ArticComponent {

    private var _appComponent: AppComponent? = null

    val appComponent: AppComponent
        get() {
            return _appComponent!! // should be set by now.
        }

    fun killComponent() {
        _appComponent = null
    }

    // TODO: Re-evaluate whether this helps in the practical sense
    fun setInternalAppComponent(appComponent: AppComponent) {
        _appComponent = appComponent
    }
}
