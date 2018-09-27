package artic.edu.localization.ui

import android.arch.lifecycle.ViewModel
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
@author Sameer Dhakal (Fuzz)
 */
@Module
abstract class LocalizationUiModule {
    @Binds
    @IntoMap
    @ViewModelKey(LanguageSettingsViewModel::class)
    abstract fun languageSettingsViewModel(accessMemberCardViewModel: LanguageSettingsViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val languageSettingsFragment: LanguageSettingsFragment

    @Module
    companion object {

        @JvmStatic
        @Provides
        fun provideLanguageSettingsPrefManager(context: Context):
                LanguageSettingsPrefManager = LanguageSettingsPrefManager(context)
    }
}