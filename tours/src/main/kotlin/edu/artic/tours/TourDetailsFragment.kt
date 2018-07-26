package edu.artic.tours

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.itemChanges
import edu.artic.base.utils.fromHtml
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_tour_details.*
import kotlin.reflect.KClass

class TourDetailsFragment : BaseViewModelFragment<TourDetailsViewModel>() {

    override val viewModelClass: KClass<TourDetailsViewModel>
        get() = TourDetailsViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_tour_details
    override val title: String
        get() = "Tours" // TODO: add to strings or figure out language stuff

    private val tour : ArticTour get() = arguments!!.getParcelable(ARG_TOUR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        val tourStopAdapter = TourDetailsStopAdapter()
        recyclerView.adapter = tourStopAdapter
        val decoration = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(view.context, R.drawable.tour_detail_tour_stop_divider)?.let {
            decoration.setDrawable(it)
        }
        recyclerView.addItemDecoration(decoration)

    }

    override fun onRegisterViewModel(viewModel: TourDetailsViewModel) {
        super.onRegisterViewModel(viewModel)
        viewModel.tour = tour
    }

    override fun setupBindings(viewModel: TourDetailsViewModel) {
        viewModel.titleText
                .subscribe {
                    expandedTitle.text = it
                    toolbarTitle.text = it
                }.disposedBy(disposeBag)
        viewModel.description
                .map { it.fromHtml() }
                .bindToMain(description.text())
                .disposedBy(disposeBag)
        viewModel.startTourButtonText
                .bindToMain(startTourButtonText.text())
                .disposedBy(disposeBag)

        startTourButton.clicks().subscribe { viewModel.onClickStartTour() }.disposedBy(disposeBag)

        val adapter = recyclerView.adapter as TourDetailsStopAdapter

        viewModel.stops
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_all_tours, menu)
    }

    companion object {
        private val ARG_TOUR = "${TourDetailsFragment::class.java.simpleName}:TOUR"

        fun argsBundle(tour: ArticTour) = Bundle().apply {
            putParcelable(ARG_TOUR, tour)
        }
    }
}