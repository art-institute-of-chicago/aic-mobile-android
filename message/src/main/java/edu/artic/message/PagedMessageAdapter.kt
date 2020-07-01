package edu.artic.message

import android.app.Activity
import android.content.res.Resources
import android.text.method.LinkMovementMethod
import android.view.View
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.AutoHolderRecyclerViewAdapter
import edu.artic.adapter.BaseViewHolder
import edu.artic.base.utils.fromHtml
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.cell_paged_message.view.*

class PagedMessageAdapter : AutoHolderRecyclerViewAdapter<PagedMessageCellViewModel>() {

    val nextButtonClicks: Subject<Unit> = PublishSubject.create()
    val previousButtonClicks: Subject<Unit> = PublishSubject.create()
    val closeButtonClicks: Subject<Unit> = PublishSubject.create()
    val actionButtonClicks: Subject<String> = PublishSubject.create()

    override fun View.onBindView(item: PagedMessageCellViewModel, position: Int) {
        messageTextView.movementMethod = LinkMovementMethod.getInstance()

        // Set top padding based on screen height
        titleTextView.setPadding(
                0,
                (Resources.getSystem().displayMetrics.heightPixels.toFloat() * 0.15f).toInt(),
                0,
                0
        )

        item.titleText
                .bindToMain(titleTextView.text())
                .disposedBy(item.viewDisposeBag)

        item.messageText
                .map { it.fromHtml(shouldRemoveAnchors = false) }
                .bindToMain(messageTextView.text())
                .disposedBy(item.viewDisposeBag)

        item.actionTitle
                .bindToMain(actionButton.text())
                .disposedBy(item.viewDisposeBag)

        item.hasAction
                .bindToMain(actionButton.visibility())
                .disposedBy(item.viewDisposeBag)

        item.isFirstPage
                .map { !it }
                .bindToMain(previousButton.visibility())
                .disposedBy(item.viewDisposeBag)

        item.isLastPage
                .map { !it }
                .bindToMain(nextButton.visibility())
                .disposedBy(item.viewDisposeBag)

        item.isLastPage
                .bindToMain(closeButton.visibility())
                .disposedBy(item.viewDisposeBag)

        actionButton
                .clicks()
                .switchMap { item.action }
                .bindTo(actionButtonClicks)
                .disposedBy(item.viewDisposeBag)

        nextButton
                .clicks()
                .bindTo(nextButtonClicks)
                .disposedBy(item.viewDisposeBag)

        previousButton
                .clicks()
                .bindTo(previousButtonClicks)
                .disposedBy(item.viewDisposeBag)

        closeButton
                .clicks()
                .bindTo(closeButtonClicks)
                .disposedBy(item.viewDisposeBag)
    }

    override fun onItemViewHolderRecycled(holder: BaseViewHolder, position: Int) {
        super.onItemViewHolderRecycled(holder, position)
        getItem(position).apply {
            cleanup()
        }
    }

    override fun getLayoutResId(position: Int) = R.layout.cell_paged_message

}
