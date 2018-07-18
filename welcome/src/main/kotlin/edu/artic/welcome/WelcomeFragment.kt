package edu.artic.welcome

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import edu.artic.adapter.itemChanges
import edu.artic.base.fileAsString
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.Observable
import kotlinx.android.synthetic.main.app_bar_layout.view.*
import kotlinx.android.synthetic.main.fragment_welcome.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {
    override val title: String
        get() = "Welcome"

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

    @Inject
    lateinit var moshi: Moshi

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * TODO:: move this logic away into the app bar view class
         * TODO:: Make a Custom AppBar view that dynamically switches the toolbar type (collapsible and non collapsible)
         */
        val appBar = appBarLayout as AppBarLayout
        (appBarLayout as AppBarLayout).apply {
            addOnOffsetChangedListener { aBarLayout, verticalOffset ->
                val progress: Double = 1 - Math.abs(verticalOffset) / aBarLayout.totalScrollRange.toDouble()
                appBar.searchIcon.background.alpha = (progress * 255).toInt()
                appBar.flagIcon.drawable.alpha = (progress * 255).toInt()
            }
        }

        context?.let {

            /* Build tour summary list*/
            viewModel.addTours(getTours())
            val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            tourSummaryRecyclerView.layoutManager = layoutManager
            val tourSummaryAdapter = WelcomeToursAdapter()
            tourSummaryRecyclerView.adapter = tourSummaryAdapter
            viewModel.tours.bindToMain(tourSummaryAdapter.itemChanges()).disposedBy(disposeBag)

            /* Build on view list*/
            viewModel.addExhibitions(getExhibitions())
            val adapter = OnViewAdapter()
            val exhibitionLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            onViewRecyclerView.layoutManager = exhibitionLayoutManager
            onViewRecyclerView.adapter = adapter
            viewModel.exhibitions.bindToMain(adapter.itemChanges()).disposedBy(disposeBag)

            /* Build event summary list*/
            viewModel.addEvents(getEvents())
            val eventsLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            eventsRecyclerView.layoutManager = eventsLayoutManager
            val eventsAdapter = WelcomeEventsAdapter()
            eventsRecyclerView.adapter = eventsAdapter
            viewModel.events.bindToMain(eventsAdapter.itemChanges()).disposedBy(disposeBag)
        }


        viewModel.shouldPeekTourSummary
                .filter { it }
                .subscribe {
                    animateRecyclerView()
                }
                .disposedBy(disposeBag)

    }

    /**
     * Peek Animation.
     * Scroll RecyclerView to the last item and back again to first item.
     */
    private fun animateRecyclerView() {

        Observable.interval(2000, 500, TimeUnit.MILLISECONDS)
                .take(2)
                .subscribe { it ->
                    if (it == 0L) {
                        tourSummaryRecyclerView.smoothScrollToPosition(1)
                    } else {
                        tourSummaryRecyclerView.smoothScrollToPosition(0)
                        viewModel.onPeekedTour()
                    }
                }
                .disposedBy(disposeBag)
    }

    /**
     * TODO:: remove this once ToursDao is ready
     */
    private fun getTours(): List<ArticTour> {

        return activity.let {
            if (it == null) {
                emptyList()
            } else {
                val toursJson = it.assets.fileAsString("json", "tours.json")
                val adapter: JsonAdapter<List<ArticTour>> = moshi.adapter(Types.newParameterizedType(List::class.java, ArticTour::class.java))
                return@let adapter.fromJson(toursJson) as List<ArticTour>
            }

        }
    }

    /**
     * TODO:: remove this once ExhibitionDao is ready
     */
    private fun getExhibitions(): List<ArticExhibition> {
        return activity.let {
            if (it == null) {
                emptyList()
            } else {
                val exhibitionJson = it.assets.fileAsString("json", "exhibitions.json")
                val adapter: JsonAdapter<List<ArticExhibition>> = moshi.adapter(Types.newParameterizedType(List::class.java, ArticExhibition::class.java))
                return@let adapter.fromJson(exhibitionJson) as List<ArticExhibition>
            }

        }
    }

    /**
     * TODO:: remove this once ExhibitionDao is ready
     */
    private fun getEvents(): List<ArticEvent> {
        return activity.let {
            if (it == null) {
                emptyList()
            } else {
                val exhibitionJson = it.assets.fileAsString("json", "events.json")
                val adapter: JsonAdapter<List<ArticEvent>> = moshi.adapter(Types.newParameterizedType(List::class.java, ArticEvent::class.java))
                return@let adapter.fromJson(exhibitionJson) as List<ArticEvent>
            }

        }
    }


}


