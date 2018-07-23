package edu.artic.ui

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.view.*
import com.fuzz.rx.DisposeBag
import dagger.android.support.AndroidSupportInjection

abstract class BaseFragment : Fragment() {

    var toolbar: Toolbar? = null

    protected abstract val title: String

    @get:LayoutRes
    protected abstract val layoutResId: Int

    val baseActivity: BaseActivity
        get() = activity as BaseActivity

    val disposeBag = DisposeBag()
    val navigationDisposeBag = DisposeBag()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }


    override fun onResume() {
        super.onResume()
        view!!.post { updateToolbar(view!!) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)

    }

    private fun updateToolbar(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        if (toolbar != null) {
            baseActivity.setSupportActionBar(toolbar)
            baseActivity.supportActionBar?.apply {
                setDisplayShowTitleEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
            }

            baseActivity.title = title
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        baseActivity.title = title
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposeBag.clear()
    }

    override fun onDestroy() {
        // fix for bug where viewmodel store is not cleared on 27.1.0, might be fixed later.
        val activity = activity
        if (activity != null
                && activity.isFinishing
                && !activity.isChangingConfigurations) {
            viewModelStore.clear()
        }
        super.onDestroy()
    }


}