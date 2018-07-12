package edu.artic.ui

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fuzz.rx.DisposeBag
import dagger.android.support.AndroidSupportInjection

abstract class BaseFragment : Fragment() {

    var toolbar: Toolbar? = null

    @get:LayoutRes
    protected abstract val layoutResId: Int

    val baseActivity: BaseActivity
        get() = activity as BaseActivity

    val disposeBag = DisposeBag()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layoutResId, container, false)
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
        return view

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    protected abstract val title: String
}