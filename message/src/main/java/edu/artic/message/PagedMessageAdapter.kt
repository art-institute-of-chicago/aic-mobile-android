package edu.artic.message

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
import edu.artic.message.databinding.CellPagedMessageBinding
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class PagedMessageAdapter : AutoHolderRecyclerViewAdapter<PagedMessageCellViewModel>() {

    val nextButtonClicks: Subject<Unit> = PublishSubject.create()
    val previousButtonClicks: Subject<Unit> = PublishSubject.create()
    val closeButtonClicks: Subject<Unit> = PublishSubject.create()
    val actionButtonClicks: Subject<String> = PublishSubject.create()

    override fun View.onBindView(
        item: PagedMessageCellViewModel,
        holder: BaseViewHolder,
        position: Int,
    ) {
        val binding = holder.binding as CellPagedMessageBinding
        binding.messageTextView.movementMethod = LinkMovementMethod.getInstance()

        // Set top padding based on screen height
        binding.titleTextView.setPadding(
            0,
            (Resources.getSystem().displayMetrics.heightPixels.toFloat() * 0.15f).toInt(),
            0,
            0
        )

        item.titleText
            .bindToMain(binding.titleTextView.text())
            .disposedBy(item.viewDisposeBag)

        item.messageText
            .map { it.fromHtml(shouldRemoveAnchors = false) }
            .bindToMain(binding.messageTextView.text())
            .disposedBy(item.viewDisposeBag)

        item.actionTitle
            .bindToMain(binding.actionButton.text())
            .disposedBy(item.viewDisposeBag)

        item.hasAction
            .bindToMain(binding.actionButton.visibility())
            .disposedBy(item.viewDisposeBag)

        item.isFirstPage
            .map { !it }
            .bindToMain(binding.previousButton.visibility())
            .disposedBy(item.viewDisposeBag)

        item.isLastPage
            .map { !it }
            .bindToMain(binding.nextButton.visibility())
            .disposedBy(item.viewDisposeBag)

        item.isLastPage
            .bindToMain(binding.closeButton.visibility())
            .disposedBy(item.viewDisposeBag)

        binding.actionButton
            .clicks()
            .switchMap { item.action }
            .bindTo(actionButtonClicks)
            .disposedBy(item.viewDisposeBag)

        binding.nextButton
            .clicks()
            .bindTo(nextButtonClicks)
            .disposedBy(item.viewDisposeBag)

        binding.previousButton
            .clicks()
            .bindTo(previousButtonClicks)
            .disposedBy(item.viewDisposeBag)

        binding.closeButton
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
