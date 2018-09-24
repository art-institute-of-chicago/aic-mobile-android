package edu.artic.info

import android.support.annotation.StringRes
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.db.daos.ArticDataObjectDao
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
        dataObjectDao: ArticDataObjectDao,
        languageSelector: LanguageSelector
) : NavViewViewModel<MuseumInformationViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        class BuyTicket(val url: String) : NavigationEndpoint()
        class CallMuseum(@StringRes val phone: Int) : NavigationEndpoint()
        class ShowMuseumInMap(@StringRes val location: Int) : NavigationEndpoint()
    }

    val museumHours: Subject<String> = BehaviorSubject.create()
    val museumAddress: Subject<Int> = BehaviorSubject.createDefault(R.string.museumAddress)
    val museumPhone: Subject<Int> = BehaviorSubject.createDefault(R.string.museumPhoneNumber)
    private val ticketUrl: Subject<String> = BehaviorSubject.create()

    init {

        generalInfoDao
                .getGeneralInfo()
                .toObservable()
                .map { it -> languageSelector.selectFrom(it.allTranslations()) }
                .map {
                    it.museumHours
                }.bindTo(museumHours)
                .disposedBy(disposeBag)

        dataObjectDao.getDataObject()
                .toObservable()
                .map { it.ticketsUrlAndroid }
                .filter { it.isNotEmpty() }
                .bindTo(ticketUrl)

    }

    fun onBuyTicketClicked() {
        ticketUrl.take(1)
                .map { url -> Navigate.Forward(NavigationEndpoint.BuyTicket(url)) }
                .bindTo(navigateTo)
                .disposedBy(disposeBag)
    }

    fun onPhoneNumberClicked() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.CallMuseum(R.string.museumPhoneNumber)))
    }

    fun onMuseumAddressClicked() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ShowMuseumInMap(R.string.museumAddress)))
    }

}