package edu.artic.ui

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fuzz.rx.DisposeBag

abstract class BaseFragment : Fragment(){

    @get:LayoutRes
    protected abstract val layoutResId : Int

    val disposeBag = DisposeBag()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }
}