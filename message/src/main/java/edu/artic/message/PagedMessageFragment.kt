package edu.artic.message

import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.db.models.ArticMessage
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_paged_message.*
import javax.inject.Inject
import kotlin.reflect.KClass

class PagedMessageFragment : BaseViewModelFragment<PagedMessageViewModel>() {
    // region - Properties -
    override val viewModelClass: KClass<PagedMessageViewModel> = PagedMessageViewModel::class
    override val title = 0
    override val layoutResId = R.layout.fragment_paged_message
    override val screenName: ScreenName? = null

    @Inject
    lateinit var customTabManager: CustomTabManager

    private val adapter = PagedMessageAdapter()
    // endregion

    // region - Lifecycle -
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = this@PagedMessageFragment.adapter
        }
        PagerSnapHelper().attachToRecyclerView(recyclerView)

        val messages = arguments
                ?.getParcelableArray(ARG_MESSAGES)
                ?.filterIsInstance(ArticMessage::class.java)
                ?: listOf()
        viewModel.update(messages)
    }

    override fun onDestroy() {
        viewModel.markMessagesAsSeen()

        super.onDestroy()
    }
    // endregion

    // region - Bindings -
    override fun setupBindings(viewModel: PagedMessageViewModel) {
        super.setupBindings(viewModel)

        viewModel
                .messages
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

        adapter
                .nextButtonClicks
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    val position = (recyclerView.layoutManager as? LinearLayoutManager)
                            ?.findFirstCompletelyVisibleItemPosition()
                            ?: 0
                    recyclerView.smoothScrollToPosition(position + 1)
                }
                .disposedBy(disposeBag)

        adapter
                .actionButtonClicks
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { action ->
                    val uri = Uri.parse(action)
                    customTabManager.openUrlOnChromeCustomTab(requireContext(), uri)
                }
                .disposedBy(disposeBag)

        adapter
                .previousButtonClicks
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    val position = (recyclerView.layoutManager as? LinearLayoutManager)
                            ?.findFirstCompletelyVisibleItemPosition()
                            ?: 0
                    recyclerView.smoothScrollToPosition(position - 1)
                }
                .disposedBy(disposeBag)

        adapter
                .closeButtonClicks
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    dismiss()
                }
                .disposedBy(disposeBag)
    }
    // endregion

    // region - Companion Object -
    companion object {
        private val ARG_MESSAGES = "${PagedMessageFragment::class.java.simpleName}: messages"

        fun create(messages: List<ArticMessage>): PagedMessageFragment =
                PagedMessageFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArray(ARG_MESSAGES, messages.toTypedArray())
                    }
                }
    }
    // endregion
}
