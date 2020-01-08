package edu.artic.info

import android.support.annotation.StringRes
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
@author Sameer Dhakal (Fuzz)
 */
class MuseumInformationViewModel @Inject constructor(
        generalInfoDao: GeneralInfoDao,
        languageSelector: LanguageSelector
) : NavViewViewModel<MuseumInformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class CallMuseum(@StringRes val phone: Int) : NavigationEndpoint()
        class ShowMuseumInMap(@StringRes val location: Int) : NavigationEndpoint()
        object Search : NavigationEndpoint()
    }

    val museumHours: Subject<String> = BehaviorSubject.create()
    val museumAddress: Subject<Int> = BehaviorSubject.createDefault(R.string.museumAddress)
    val museumPhone: Subject<Int> = BehaviorSubject.createDefault(R.string.museumPhoneNumber)

    init {

        generalInfoDao
                .getGeneralInfo()
                .toObservable()
                .map { languageSelector.selectFrom(it.allTranslations()) }
                .map { it.museumHours }
                .bindTo(museumHours)
                .disposedBy(disposeBag)

    }

    fun onPhoneNumberClicked() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.CallMuseum(R.string.museumPhoneNumber)))
    }

    fun onMuseumAddressClicked() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ShowMuseumInMap(R.string.museumAddress)))
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

}