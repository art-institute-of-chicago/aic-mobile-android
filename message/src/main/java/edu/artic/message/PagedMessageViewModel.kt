package edu.artic.message

import com.fuzz.rx.DisposeBag
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.AppDataManager
import edu.artic.db.models.ArticMessage
import edu.artic.localization.LanguageSelector
import edu.artic.membership.MemberInfoPreferencesManager
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.CellViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class PagedMessageViewModel @Inject constructor(
        private val appDataManager: AppDataManager,
        private val memberInfoPreferencesManager: MemberInfoPreferencesManager,
        private val messagePreferencesManager: MessagePreferencesManager,
        private val languageSelector: LanguageSelector
) : BaseViewModel() {
    private var currentMessages: List<ArticMessage> = listOf()
    val messages: Subject<List<PagedMessageCellViewModel>> = BehaviorSubject.createDefault(listOf())

    fun update(messages: List<ArticMessage>) {
        val messageSize = messages.size

        disposeBag.clear()

        languageSelector
                .currentLanguage
                .map {
                    messages.mapIndexed { index, message ->
                        val translation = languageSelector.selectFrom(message.allTranslations)
                        PagedMessageCellViewModel(
                                adapterDisposeBag = viewDisposeBag,
                                message = message.let {
                                    ArticMessage(
                                            nid = it.nid,
                                            title = translation.title,
                                            messageType = it.messageType,
                                            expirationThreshold = it.expirationThreshold,
                                            tourExit = it.tourExit,
                                            isPersistent = it.isPersistent,
                                            message = replaceMemberDetails(translation.message),
                                            action = replaceMemberDetails(it.action),
                                            actionTitle = translation.actionTitle,
                                            translations = it.translations
                                    )
                                },
                                isFirstPage = index == 0,
                                isLastPage = index == messageSize - 1
                        )
                    }
                }
                .bindTo(this.messages)
                .disposedBy(disposeBag)

        currentMessages = messages
    }

    fun markMessagesAsSeen() {
        messagePreferencesManager.markMessagesAsSeen(currentMessages)
    }

    private fun replaceMemberDetails(string: String?): String? {
        val memberInfo = appDataManager.currentMemberInfo ?: return string
        val zipCode = memberInfoPreferencesManager.memberZipCode ?: return string

        return string?.replace("%CARD_ID%", memberInfo.primaryConstituentID ?: "")
                ?.replace("%ZIP_CODE%", zipCode)
                ?.replace("%NAME%", memberInfo.cardHolder ?: "")
                ?.replace("%FIRST_NAME%", memberInfo.cardHolder?.split(" ")?.firstOrNull() ?: "")
                ?.replace("%EXPIRATION_DATE%", memberInfo.expiration ?: "")
    }
}

class PagedMessageCellViewModel(
        adapterDisposeBag: DisposeBag,
        message: ArticMessage,
        isFirstPage: Boolean,
        isLastPage: Boolean
) : CellViewModel(adapterDisposeBag) {
    val titleText: Subject<String> = BehaviorSubject.createDefault(message.title ?: "")
    val messageText: Subject<String> = BehaviorSubject.createDefault(message.message ?: "")
    val actionTitle: Subject<String> = BehaviorSubject.createDefault(message.actionTitle ?: "")
    val action: Subject<String> = BehaviorSubject.createDefault(message.action ?: "")
    val hasAction: Subject<Boolean> = BehaviorSubject.createDefault(message.action != null)
    val isFirstPage: Subject<Boolean> = BehaviorSubject.createDefault(isFirstPage)
    val isLastPage: Subject<Boolean> = BehaviorSubject.createDefault(isLastPage)
}
