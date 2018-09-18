package edu.artic.info

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.info.AccessMemberCardViewModel.DisplayMode
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Validates the user provided member information.
 * Switches [AccessMemberCardFragment] view state based on [DisplayMode].
 *
 * When [DisplayMode]
 * * is [DisplayMode.DisplayForm] form is displayed
 * * is [DisplayMode.UpdateForm] the filled form is displayed
 * * is [DisplayMode.DisplayAccessCard] the form is hidden and member information is presented with
 * barcode.
 *
 *
 * @author Sameer Dhakal (Fuzz)
 */
class AccessMemberCardViewModel @Inject constructor(
        private val service: RetrofitMemberDataProvider,
        private val infoPreferencesManager: MemberInfoPreferencesManager,
        private val analyticsTracker: AnalyticsTracker
) : BaseViewModel() {

    sealed class DisplayMode {
        object DisplayForm : DisplayMode()
        class DisplayAccessCard(val memberID: String) : DisplayMode()
        class UpdateForm(val memberID: String, val zipCode: String) : DisplayMode()
    }

    /**
     * Emits events for various loading states.
     */
    var loadStatus: BehaviorSubject<LoadStatus> = BehaviorSubject.createDefault<LoadStatus>(LoadStatus.None())

    /**
     * Form field events.
     */
    var zipCode = BehaviorSubject.createDefault<String>("")
    var memberID = BehaviorSubject.createDefault<String>("")
    var isValid = BehaviorSubject.createDefault<Boolean>(false)


    var displayMode: Subject<DisplayMode> = BehaviorSubject.create()


    private var members: Subject<List<MemberInfo>> = BehaviorSubject.create()
    private var selectedMember: Subject<MemberInfo> = BehaviorSubject.create()

    var expiration: Subject<String> = BehaviorSubject.create()
    var selectedCardHolder: Subject<String> = BehaviorSubject.create()
    var membership: Subject<String> = BehaviorSubject.create()

    init {

        /**
         * Validate the form and enable the sign in button.
         */
        Observables.combineLatest(
                zipCode,
                memberID)
                .map { (zipCode, memberID) ->
                    zipCode.isNotEmpty() && memberID.isNotEmpty()

                }
                .bindTo(isValid)
                .disposedBy(disposeBag)

        /**
         * Updated expiration and membership data when cardHolder is switched.
         */
        selectedMember
                .subscribeBy { memberInfo ->
                    expiration.onNext(memberInfo.expiration.orEmpty())
                    selectedCardHolder.onNext(memberInfo.cardHolder.orEmpty())
                    membership.onNext(memberInfo.memberLevel.orEmpty())

                    memberInfo.primaryConstituentID?.let {
                        displayMode.onNext(DisplayMode.DisplayAccessCard(it))
                        /**
                         * Log show_card analytics event.
                         */
                        analyticsTracker.reportEvent(EventCategoryName.Member, AnalyticsAction.memberShowCard)
                    }
                }.disposedBy(disposeBag)


        val savedMemberID = infoPreferencesManager.memberID
        val savedMemberZipCode = infoPreferencesManager.memberZipCode

        /**
         * If the Membership data is available in shared pref file, validate it automatically.
         */
        if (!savedMemberID.isNullOrBlank() && !savedMemberZipCode.isNullOrBlank()) {
            loadStatus.onNext(LoadStatus.Loading())
            service.getMemberData(savedMemberID!!, savedMemberZipCode!!)
                    .subscribeBy(
                            onNext = { serverResponse ->
                                onMemberInformationValidated(serverResponse)

                            },
                            onError = {
                                loadStatus.onNext(LoadStatus.Error(it))
                            },
                            onComplete = {
                                loadStatus.onNext(LoadStatus.None())
                            })
                    .disposedBy(disposeBag)
        } else {
            displayMode.onNext(DisplayMode.DisplayForm)
        }

    }

    /**
     * Validate the user provided member information.
     */
    fun onSignInClick() {
        Observables.combineLatest(zipCode, memberID)
                .take(1)
                .flatMap { (zipCode, memberID) ->
                    loadStatus.onNext(LoadStatus.Loading())
                    service.getMemberData(memberID, zipCode)
                            .withLatestFrom(Observable.just(memberID to zipCode))
                }
                .subscribeBy(
                        onNext = { (serverResponse, memberDetails) ->
                            val fault = serverResponse.responseBody?.fault

                            if (fault == null) {
                                /**
                                 * Save validated member details.
                                 **/
                                val (memberID, zipCode) = memberDetails
                                infoPreferencesManager.memberZipCode = zipCode
                                infoPreferencesManager.memberID = memberID
                                onMemberInformationValidated(serverResponse)
                            } else {
                                val errorMessage = fault.faultCode
                                loadStatus.onNext(LoadStatus.Error(Throwable(errorMessage)))
                            }
                        },
                        onError = {
                            if (it is UnknownHostException) {
                                /**
                                 * TODO:: Needs Refactoring
                                 * We should handle network error globally since it effects other parts of app too.
                                 */
                                loadStatus.onNext(LoadStatus.Error(Throwable("Network issue, please try again.")))
                            } else {
                                loadStatus.onNext(LoadStatus.Error(it))
                            }
                        },
                        onComplete = {
                            loadStatus.onNext(LoadStatus.None())
                        })
                .disposedBy(disposeBag)
    }

    private fun onMemberInformationValidated(serverResponse: SOAPMemberInfoResponse) {
        loadStatus.onNext(LoadStatus.None())

        serverResponse.responseBody?.soapResponse?.memberResponseObject?.let { memberResponseObject ->
            val accountMembers = mutableListOf<MemberInfo>()

            memberResponseObject.members?.member1?.let {
                accountMembers.add(it)
            }

            memberResponseObject.members?.member2?.let {
                accountMembers.add(it)
            }

            members.onNext(accountMembers)

            /**
             * Always select the first member as the selected member.
             */
            if (accountMembers.isNotEmpty()) {
                selectedMember.onNext(accountMembers.first())
            }

        }
    }

    /**
     * Switch the Cardholder.
     */
    fun onSwitchCardholderClicked() {
        Observables.combineLatest(members, selectedCardHolder)
                .take(1)
                .subscribeBy { (members, currentCardHolder) ->
                    members.forEach {
                        if (it.cardHolder != currentCardHolder) {
                            selectedMember.onNext(it)
                        }
                    }
                }.disposedBy(disposeBag)
    }

    /**
     * Present the member information form so that user can update the information.
     */
    fun onUpdateInformationClicked() {
        val memberID = infoPreferencesManager.memberID
        val memberZipCode = infoPreferencesManager.memberZipCode
        if (memberID != null && memberZipCode != null) {
            displayMode.onNext(DisplayMode.UpdateForm(memberID, memberZipCode))
        }
    }

}
