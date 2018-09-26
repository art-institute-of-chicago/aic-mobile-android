package artic.edu.localization.ui

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
@author Sameer Dhakal (Fuzz)
 */
@Module
abstract class LocalizationUiModule{
    @Binds
    @IntoMap
    @ViewModelKey(LanguageSettingsViewModel::class)
    abstract fun languageSettingsViewModel(accessMemberCardViewModel: LanguageSettingsViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val languageSettingsFragment: LanguageSettingsFragment
}