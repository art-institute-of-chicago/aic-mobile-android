package edu.artic.membership

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

@Module
abstract class AccessMemberModule {

    @Binds
    @IntoMap
    @ViewModelKey(AccessMemberCardViewModel::class)
    abstract fun accessMemberCardViewModel(accessMemberCardViewModel: AccessMemberCardViewModel):
            ViewModel

    @get:ContributesAndroidInjector
    abstract val accessMemberCardFragment: AccessMemberCardFragment
}