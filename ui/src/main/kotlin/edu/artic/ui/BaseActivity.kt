package edu.artic.ui

import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.fuzz.rx.DisposeBag
import com.fuzz.rx.disposedBy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import edu.artic.localization.LanguageSelector
import edu.artic.localization.primaryLocale
import java.lang.reflect.ParameterizedType
import javax.inject.Inject

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(), HasSupportFragmentInjector {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding is not available yet.")

    val disposeBag = DisposeBag()
    val navDisposeBag = DisposeBag()

    open val useInjection: Boolean
        get() = true

    open val shouldRecreateUponLanguageChange: Boolean
        get() = true

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var languageSelector: LanguageSelector

    /**
     * Utility function for retrieving the primary [NavController] of
     * this [android.app.Activity].
     */
    val navController: NavController
        get() = Navigation.findNavController(this, R.id.container)

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        if (useInjection) {
            AndroidInjection.inject(this)

            languageSelector.currentLanguage
                .take(1)
                .subscribe {
                    ensureConfigIncludesAppLocale()
                }.disposedBy(disposeBag)

            languageSelector.currentLanguage
                // Ignore current value; we only want to receive updates
                .skip(1)
                .subscribe {
                    ensureConfigIncludesAppLocale()
                    if (shouldRecreateUponLanguageChange) {
                        recreate()
                    }
                }.disposedBy(disposeBag)
        }
        super.onCreate(savedInstanceState)

        val vbClass = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<*>
        val method = vbClass.getDeclaredMethod("inflate", LayoutInflater::class.java)
        _binding = method.invoke(null, layoutInflater) as VB

        setContentView(binding.root)

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
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
        _binding = null
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
            if (lastFragment == null || lastFragment !is BaseFragment<*> || !lastFragment.onBackPressed()) {
                super.onBackPressed()
            }
        }

    }

    fun isRootFragment(inId: Int): Boolean {
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (navController.currentDestination?.id == inId) {
                return true
            }
        }
        return false
    }
}

