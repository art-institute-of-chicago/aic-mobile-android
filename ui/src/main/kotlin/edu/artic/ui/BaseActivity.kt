package edu.artic.ui

import android.support.v7.app.AppCompatActivity
import com.fuzz.rx.DisposeBag

abstract class BaseActivity : AppCompatActivity(){

    val disposeBag = DisposeBag()

}
