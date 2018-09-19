package edu.artic.info


import android.os.Bundle
import android.view.View
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_language_settings.*
import java.util.*
import kotlin.reflect.KClass

class LanguageSettingsFragment : BaseViewModelFragment<LanguageSettingsViewModel>() {
    override val viewModelClass: KClass<LanguageSettingsViewModel> = LanguageSettingsViewModel::class
    override val title: String = "Language Settings"
    override val layoutResId: Int = R.layout.fragment_language_settings
    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.LanguageSettings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        englishLanguage.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onEnglishLanguageSelected()
                }.disposedBy(disposeBag)

        spanishLanguage.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onSpanishLanguageSelected()
                }.disposedBy(disposeBag)


        chineseLanguage.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onChineseLanguageSelected()
                }.disposedBy(disposeBag)

    }

    override fun setupBindings(viewModel: LanguageSettingsViewModel) {
        super.setupBindings(viewModel)

        viewModel.selectedLanguage
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        Locale.ENGLISH -> {
                            englishLanguage.isChecked = true
                        }
                        Locale("es") -> {
                            spanishLanguage.isChecked = true
                        }
                        Locale.CHINESE -> {
                            chineseLanguage.isChecked = true
                        }
                    }
                }.disposedBy(disposeBag)
    }

}
