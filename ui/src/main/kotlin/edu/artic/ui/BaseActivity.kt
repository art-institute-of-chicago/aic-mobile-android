package edu.artic.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import edu.artic.localization.LanguageSelector
import edu.artic.localization.primaryLocale
import java.util.*
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), HasSupportFragmentInjector {

    @get:LayoutRes
    protected abstract val layoutResId: Int

    val disposeBag = DisposeBag()
    val navDisposeBag = DisposeBag()

    open val useInjection: Boolean
        get() = true

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var languageSelector: LanguageSelector

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        if (useInjection) {
            AndroidInjection.inject(this)
            languageSelector.currentLanguage
                    .subscribe {
                        ensureConfigIncludesAppLocale()
                        recreate()
                    }.disposedBy(disposeBag)
        }
        super.onCreate(savedInstanceState)
        if (layoutResId != 0) {
            setContentView(layoutResId)
        }
    }

    override fun onResume() {
        super.onResume()
        if (useInjection) {
            ensureConfigIncludesAppLocale()
        }
    }

    @Suppress("DEPRECATION")
    @UiThread
    fun ensureConfigIncludesAppLocale() {
        val appLocale = languageSelector.getAppLocale()
        if (appLocale != resources.configuration.primaryLocale) {
            resources.updateConfiguration(Configuration().apply {
                primaryLocale = appLocale
            }, resources.displayMetrics)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposeBag.clear()
    }

    /**
     * Addition allows main child fragment the ability to handle its own back press,
     * if not handled go to default back press, using the primaryNavigationFragment's childManager.
     *
     * This was what I found worked with current implementation.
     */
    override fun onBackPressed() {
        val primaryNavFragment: Fragment? = supportFragmentManager.primaryNavigationFragment

        if (primaryNavFragment == null || primaryNavFragment.childFragmentManager.fragments.size <= 0) {
            super.onBackPressed()
        } else {
            val lastFragment = primaryNavFragment.childFragmentManager.fragments.last()
            if (lastFragment == null || lastFragment !is BaseFragment || !lastFragment.onBackPressed()) {
                super.onBackPressed()
            }
        }

    }

}

