package edu.artic.localization.ui


//import kotlinx.android.synthetic.main.fragment_language_settings.*
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintSet
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.dpToPixels
import edu.artic.localization.SPANISH
import edu.artic.localization.ui.databinding.FragmentLanguageSettingsBinding
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*
import kotlin.math.max
import kotlin.reflect.KClass


class LanguageSettingsFragment :
    BaseViewModelFragment<FragmentLanguageSettingsBinding, LanguageSettingsViewModel>() {
    override val viewModelClass: KClass<LanguageSettingsViewModel> =
        LanguageSettingsViewModel::class
    override val title = R.string.language_settings_title
    override val screenName: ScreenName? = ScreenName.LanguageSettings

    /**
     * Represents [LanguageSettingsFragment] is loaded in Splash.
     * Normally, when user selects app locale using this fragment, we recreate the host activity
     * to update the activity with selected locale.
     * No need to recreate the activity in splash mode ([LanguageSettingsFragment] is dismissed
     * upon language selection).
     */
    private val splashMode by lazy { arguments?.getBoolean(ARG_LANGUAGE_SETTINGS) ?: false }

    private val availableLocales =
        listOf(Locale.ENGLISH, SPANISH, Locale.CHINESE, Locale.KOREAN, Locale.FRENCH)

    override val overrideStatusBarColor: Boolean
        get() = !splashMode

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        availableLocales
            .forEach { locale ->
                val radioButton = RadioButton(
                    ContextThemeWrapper(requireContext(), R.style.LanguageButton),
                    null,
                    R.style.LanguageButton
                )
                radioButton.setText(
                    when (locale) {
                        Locale.ENGLISH -> R.string.localization_english_in_english
                        SPANISH -> R.string.localization_spanish_in_spanish
                        Locale.CHINESE -> R.string.localization_chinese_in_chinese
                        Locale.KOREAN -> R.string.localization_korean_in_korean
                        Locale.FRENCH -> R.string.localization_french_in_french
                        else -> R.string.localization_english_in_english
                    }
                )
                binding.languageSelectionButtons.addView(
                    radioButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.dpToPixels(50.0f).toInt()
                    ).apply {
                        setMargins(
                            resources.getDimensionPixelSize(R.dimen.marginDouble),
                            resources.getDimensionPixelSize(R.dimen.marginQuad),
                            resources.getDimensionPixelSize(R.dimen.marginDouble),
                            0
                        )
                        gravity = Gravity.CENTER
                    }
                )

                radioButton
                    .clicks()
                    .defaultThrottle()
                    .subscribe { viewModel.changeLocale(locale) }
                    .disposedBy(disposeBag)
            }

        configureToolbar()
    }

    /**
     * Configure the fragment based on current theme.
     */
    private fun configureToolbar() {

        val a = requireContext().theme.obtainStyledAttributes(
            R.styleable.LanguageSettings
        )
        val hasToolbar =
            a.getBoolean(R.styleable.LanguageSettings_languageSettingsContainsToolbar, true)
        val hasDivider = a.getBoolean(R.styleable.LanguageSettings_languageSettingsHasDivider, true)
        val verticalBias =
            a.getFloat(R.styleable.LanguageSettings_languageSettingsButtonVerticalBias, 0f)

        a.recycle()

        if (hasToolbar) {
            binding.appBar?.visibility = View.VISIBLE
        } else {
            binding.appBar?.visibility = View.GONE
        }

        if (hasDivider) {
            binding.divider.visibility = View.VISIBLE
        } else {
            binding.divider.visibility = View.GONE
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintContainer)
        constraintSet.setVerticalBias(R.id.languageSelectionButtons, verticalBias)
        constraintSet.applyTo(binding.constraintContainer)
    }

    override fun setupBindings(viewModel: LanguageSettingsViewModel) {
        super.setupBindings(viewModel)

        requireActivity().title = resources.getString(R.string.language_settings_title)

        binding.searchIcon.clicks()
            .defaultThrottle()
            .subscribe {
                val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                startActivity(intent)
            }.disposedBy(disposeBag)

        viewModel
            .appLocale
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { locale ->
                val index = max(availableLocales.indexOf(locale), 0)
                val radioButton = binding.languageSelectionButtons.getChildAt(index) as? RadioButton
                    ?: return@subscribe
                radioButton.isChecked = true
            }
            .disposedBy(disposeBag)

        /**
         * Only listen to new locale change.
         */
        if (splashMode) {
            viewModel.selectedLocale
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dismissDialog()
                }.disposedBy(disposeBag)
        }
    }

    @UiThread
    private fun dismissDialog() {
        requireView()
            .animate()
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                callback?.languageSelected()
                dismiss()
            }

    }


    companion object {
        private const val ARG_LANGUAGE_SETTINGS = "ARG_LANGUAGE_SETTINGS"

        /**
         * Factory method for the creating DialogFragment.
         */
        fun getLanguageSettingsDialogForSplash(): LanguageSettingsFragment {
            return LanguageSettingsFragment().apply {
                isCancelable = false
                arguments = Bundle().apply {
                    putBoolean(ARG_LANGUAGE_SETTINGS, true)
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
