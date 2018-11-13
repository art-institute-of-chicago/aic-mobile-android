package edu.artic.splash

import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.AppDataManager
import edu.artic.db.AppDataPreferencesManager
import edu.artic.db.ProgressDataState
import edu.artic.localization.ui.LanguageSettingsPrefManager
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashViewModel @Inject constructor(
        appDataManager: AppDataManager,
        analyticsTracker: AnalyticsTracker,
        private val appDaPrefManager: AppDataPreferencesManager,
        private val languageSettingsPrefManager: LanguageSettingsPrefManager
) : NavViewViewModel<SplashViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class StartVideo(val displayLanguageSettings: Boolean) : NavigationEndpoint()
        object Welcome : NavigationEndpoint()
    }


    val percentage: Subject<Float> = BehaviorSubject.createDefault(0f)

    init {
        analyticsTracker.clearSession()
        appDataManager.loadData()
                .subscribe({
                    when (it) {
                        is ProgressDataState.Interrupted -> {
                            percentage.onError(it.error)
                        }
                        is ProgressDataState.Downloading -> {
                            percentage.onNext(it.progress)
                        }
                        is ProgressDataState.Done<*> -> {
                            percentage.onNext(1.0f)
                            startVideo()
                        }
                        is ProgressDataState.Empty -> {
                            startVideo()
                        }
                    }
                }, {
                    percentage.onError(it)
                }, {}).disposedBy(disposeBag)
    }

    /**
     * Play the museum floor animation video.
     */
    private fun startVideo() {
        val seenLanguageSettingsDialogBefore = languageSettingsPrefManager.userSelectedLanguage
        Navigate.Forward(NavigationEndpoint.StartVideo(!seenLanguageSettingsDialogBefore))
                .asObservable().delay(1, TimeUnit.SECONDS)
                .bindTo(navigateTo)
                .disposedBy(disposeBag)
    }

    /**
     * Allow user to proceed forward if app has cache data.
     */
    fun proceedToWelcomePageIfDataAvailable() {
        if (appDaPrefManager.downloadedNecessaryData) {
            Navigate.Forward(NavigationEndpoint.Welcome)
                    .asObservable()
                    .bindTo(navigateTo)
                    .disposedBy(disposeBag)
        }
    }
}