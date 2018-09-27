package artic.edu.localization.ui


import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.view.View
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenCategoryName
import edu.artic.localization.SPANISH
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_language_settings.*
import java.util.*
import kotlin.reflect.KClass


class LanguageSettingsFragment : BaseViewModelFragment<LanguageSettingsViewModel>() {
    override val viewModelClass: KClass<LanguageSettingsViewModel> = LanguageSettingsViewModel::class
    override val title = R.string.languageSettings
    override val layoutResId: Int = R.layout.fragment_language_settings
    override val screenCategory: ScreenCategoryName? = ScreenCategoryName.LanguageSettings

    private val splashMode by lazy { arguments!!.getBoolean(ARG_LANGUAGE_SETTINGS) }

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

        configureToolbar()
    }

    private fun configureToolbar() {

        val a = requireContext().theme.obtainStyledAttributes(
                R.styleable.LanguageSettings
        )
        val hasToolbar = a.getBoolean(R.styleable.LanguageSettings_languageSettingsContainsToolbar, true)
        val hasDivider = a.getBoolean(R.styleable.LanguageSettings_languageSettingsHasDivider, true)
        val verticalBias = a.getFloat(R.styleable.LanguageSettings_languageSettingsButtonVerticalBias, 0f)

        a.recycle()

        if (hasToolbar) {
            appBar?.visibility = View.VISIBLE
        } else {
            appBar?.visibility = View.GONE
        }

        if (hasDivider) {
            divider.visibility = View.VISIBLE
        } else {
            divider.visibility = View.GONE
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintContainer)
        constraintSet.setVerticalBias(R.id.languageSelectionButtons, verticalBias)
        constraintSet.applyTo(constraintContainer)
    }

    override fun setupBindings(viewModel: LanguageSettingsViewModel) {
        super.setupBindings(viewModel)

        requireActivity().title = resources.getString(R.string.languageSettings)

        viewModel.appLocale
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    requireActivity().title = resources.getString(R.string.languageSettings)
                    when (it) {
                        Locale.ENGLISH -> {
                            englishLanguage.isChecked = true
                        }
                        SPANISH -> {
                            spanishLanguage.isChecked = true
                        }
                        Locale.CHINESE -> {
                            chineseLanguage.isChecked = true
                        }
                    }
                }.disposedBy(disposeBag)

        /**
         * Only listen to new locale change.
         */
        viewModel.selectedLocale
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    callback?.languageSelected()
                    dismiss()
                }.disposedBy(disposeBag)
    }


    companion object {
        private const val ARG_LANGUAGE_SETTINGS = "ARG_LANGUAGE_SETTINGS"

        /**
         * Factory method for the creating DialogFragment.
         */
        fun makeFragmentForSplashMode(splashMode: Boolean): LanguageSettingsFragment {
            return LanguageSettingsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_LANGUAGE_SETTINGS, splashMode)
                }
            }
        }
    }

    interface LanguageSelectionListener {
        fun languageSelected()
    }

    private var callback: LanguageSelectionListener? = null

    fun attachTourStateListener(listener: LanguageSelectionListener) {
        callback = listener
    }
}
