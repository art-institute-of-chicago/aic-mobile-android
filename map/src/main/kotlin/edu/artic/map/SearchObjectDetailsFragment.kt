package edu.artic.map

import android.os.Bundle
import android.view.View
import com.fuzz.indicator.OffSetHint
import com.fuzz.rx.bindTo
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.mapOptional
import com.jakewharton.rxbinding2.support.v4.view.pageSelections
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import edu.artic.adapter.toPagerAdapter
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.getAudioServiceObservable
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_search_object_details.*
import kotlin.reflect.KClass

/**
 * Displays the object details of the searched item.
 * @author Sameer Dhakal (Fuzz)
 */
class SearchObjectDetailsFragment : BaseViewModelFragment<SearchObjectDetailsViewModel>() {

    override val viewModelClass: KClass<SearchObjectDetailsViewModel>
        get() = SearchObjectDetailsViewModel::class

    override val title = R.string.noTitle

    override val layoutResId: Int
        get() = R.layout.fragment_search_object_details


    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Search

    private var audioService: Subject<AudioPlayerService> = BehaviorSubject.create()
    private val adapter = SearchedObjectsAdapter()

    override fun setupBindings(viewModel: SearchObjectDetailsViewModel) {

        /**
         * Bind the viewHolders to adapter.
         */
        viewModel.searchedObjectViewModels
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter.setItemsList(it)
                    adapter.notifyDataSetChanged()
                }
                .disposedBy(disposeBag)

        /**
         * Hide the viewPagerIndicator if there's single item
         */
        viewModel.searchedObjectViewModels
                .map { it.size > 1 }
                .bindTo(viewPagerIndicator.visibility())
                .disposedBy(disposeBag)

        viewModel.playerControl
                .observeOn(AndroidSchedulers.mainThread())
                .withLatestFrom(audioService) { playControl, service ->
                    playControl to service
                }.subscribeBy { (playControl, service) ->
                    when (playControl) {
                        is SearchObjectBaseViewModel.PlayerAction.Play -> {
                            if (playControl.audioFileModel != null) {
                                service.playPlayer(playControl.requestedObject, playControl.audioFileModel)
                            } else {
                                service.playPlayer(playControl.requestedObject)
                            }
                        }
                        is SearchObjectBaseViewModel.PlayerAction.Pause -> service.pausePlayer()
                    }
                }.disposedBy(disposeBag)

        audioService
                .flatMap { service -> service.audioPlayBackStatus }
                .bindTo(viewModel.audioPlayBackStatus)
                .disposedBy(disposeBag)

        audioService
                .flatMap { service -> service.currentTrack }
                .mapOptional()
                .bindTo(viewModel.currentTrack)
                .disposedBy(disposeBag)

        /**
         * Stop the audio player if artwork audio on progress.
         */
        viewModel.leftSearchMode
                .filter { it }
                .withLatestFrom(audioService)
                .subscribeBy { (_, service) ->
                    service.stopPlayer()
                }.disposedBy(disposeBag)

        searchResults.pageSelections()
                .distinctUntilChanged()
                .bindTo(viewModel.currentPage)
                .disposedBy(disposeBag)
    }

    override fun onResume() {
        super.onResume()
        /**
         * Bind the searched object or type to viewModel.
         * Search Object and Search type are mutually exclusive.
         */
        viewModel.viewResumed(getLatestTourObject(), getAmenityType())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getAudioServiceObservable()
                .bindTo(audioService)
                .disposedBy(disposeBag)

        searchResults.adapter = adapter.toPagerAdapter()
        viewPagerIndicator.setViewPager(searchResults)
        viewPagerIndicator.setOffsetHints(OffSetHint.IMAGE_ALPHA)

        /**
         * Leave search mode when user taps close button.
         */
        close.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.leaveSearchMode()
                }.disposedBy(disposeBag)
    }


    companion object {
        private val ARG_SEARCH_OBJECT = "ARG_SEARCH_OBJECT"
        private val ARG_AMENITY_TYPE = "ARG_AMENITY_TYPE"

        fun loadArtworkResults(articObject: ArticSearchArtworkObject): SearchObjectDetailsFragment {
            return SearchObjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SEARCH_OBJECT, articObject)
                }
            }
        }

        fun loadAmenitiesByType(type: String): SearchObjectDetailsFragment {
            return SearchObjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_AMENITY_TYPE, type)
                }
            }
        }
    }

    private fun getLatestTourObject(): ArticSearchArtworkObject? {
        val data = arguments?.getParcelable<ArticSearchArtworkObject>(ARG_SEARCH_OBJECT)
        arguments?.remove(ARG_SEARCH_OBJECT)
        return data

    }

    private fun getAmenityType(): String? {
        val data = arguments?.getString(ARG_AMENITY_TYPE)
        arguments?.remove(ARG_AMENITY_TYPE)
        return data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPagerIndicator.setViewPager(null)
    }

}