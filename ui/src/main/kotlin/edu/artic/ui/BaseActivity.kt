package edu.artic.ui

import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fuzz.rx.DisposeBag
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
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

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        if (useInjection) {
            AndroidInjection.inject(this)
        }
        super.onCreate(savedInstanceState)
        if (layoutResId != 0) {
            setContentView(layoutResId)
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
     * Addition allows main child fragment the ability to handle it's own back press,
     * if not handled go to default back press, using the primaryNavigationFragments childManager
     * was what I found worked with current implementation.
     * TODO: change name to better explain why we are using this
     */
    override fun onBackPressed() {
        val primaryNavFragmentChildFragmentManager = supportFragmentManager.primaryNavigationFragment.childFragmentManager
        if (primaryNavFragmentChildFragmentManager.fragments.size <= 0) {
            super.onBackPressed()
        } else {
            val lastFragment = primaryNavFragmentChildFragmentManager.fragments.last()
            if (lastFragment == null || lastFragment !is BaseFragment || !lastFragment.onBackPressed()) {
                super.onBackPressed()
            }
        }

    }

}

/**
 * Use this to access the [androidx.navigation.NavController] hidden
 * within each of our [FragmentManager]s. Only works while the
 * activity's layout is inflated (i.e. between [BaseActivity.onCreate]
 * and [BaseActivity.onDestroy]).
 *
 * Analogous to [androidx.navigation.fragment.findNavController].
 */
fun FragmentManager.findNavController(): NavController? {
    val navFragment = primaryNavigationFragment
    return (navFragment as? NavHostFragment)?.navController
}
