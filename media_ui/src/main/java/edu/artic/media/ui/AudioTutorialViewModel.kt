package edu.artic.media.ui

import com.fuzz.rx.asObservable
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class AudioTutorialViewModel @Inject constructor() : NavViewViewModel<AudioTutorialViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object Finish : NavigationEndpoint()

    }

    fun onOkClicked() {
        Navigate.Forward(NavigationEndpoint.Finish)
                .asObservable()
                .bindTo(navigateTo)
                .disposedBy(disposeBag)
    }

}
