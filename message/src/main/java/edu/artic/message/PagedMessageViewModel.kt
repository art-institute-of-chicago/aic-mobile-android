package edu.artic.message

import com.fuzz.rx.DisposeBag
import edu.artic.db.models.ArticMessage
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.CellViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class PagedMessageViewModel @Inject constructor(
        private val messagePreferencesManager: MessagePreferencesManager
) : BaseViewModel() {
    private var currentMessages: List<ArticMessage> = listOf()
    val messages: Subject<List<PagedMessageCellViewModel>> = BehaviorSubject.createDefault(listOf())

    fun update(messages: List<ArticMessage>) {
        val messageSize = messages.size
        this.messages.onNext(
                messages.mapIndexed { index, message ->
                    PagedMessageCellViewModel(
                            adapterDisposeBag = viewDisposeBag,
                            message = message,
                            isFirstPage = index == 0,
                            isLastPage = index == messageSize - 1
                    )
                }
        )

        currentMessages = messages
    }

    fun markMessagesAsSeen() {
        messagePreferencesManager.markMessagesAsSeen(currentMessages)
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
