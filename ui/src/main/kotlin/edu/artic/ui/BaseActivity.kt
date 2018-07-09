package edu.artic.ui

import android.os.Bundle
import android.os.PersistableBundle
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import com.fuzz.rx.DisposeBag

abstract class BaseActivity : AppCompatActivity(){

    @get:LayoutRes
    protected abstract val layoutResId: Int

    val disposeBag = DisposeBag()


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        if(layoutResId != 0) {
            setContentView(layoutResId)
        }
    }
}
