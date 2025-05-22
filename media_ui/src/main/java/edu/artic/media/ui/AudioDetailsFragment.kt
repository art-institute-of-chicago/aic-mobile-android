package edu.artic.media.ui


import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.LinearLayout.LayoutParams
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.math.MathUtils
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.itemSelections
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.*
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.filterHtmlEncodedText
import edu.artic.base.utils.show
import edu.artic.db.models.ArticTour
import edu.artic.db.models.AudioFileModel
import edu.artic.db.models.getIntroStop
import edu.artic.image.listenerAnimateSharedTransaction
import edu.artic.language.LanguageAdapter
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.databinding.FragmentAudioDetailsBinding
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlin.reflect.KClass

/**
 * Extensive details about a specific [ArticObject][edu.artic.db.models.ArticObject].
 *
 * This object is expected (but as it stands, not *required* per se) to have a
 * valid [audio file][edu.artic.db.models.ArticAudioFile], which is then used
 * to fill out the interesting properties in a backing [AudioDetailsViewModel].
 *
 * This class maintains a strong connection to [AudioPlayerService] for playback
 * and a minimal amount of caching.
 *
 * @author Sameer Dhakal (Fuzz)
 */
class AudioDetailsFragment :
    BaseViewModelFragment<FragmentAudioDetailsBinding, AudioDetailsViewModel>() {
    private var exoTranslationSelector: Spinner? = null
    private var exoPause: View? = null
    private var exoPlay: View? = null

    override val viewModelClass: KClass<AudioDetailsViewModel>
        get() = AudioDetailsViewModel::class

    override val title = R.string.global_empty_string

    override val screenName: ScreenName
        get() = ScreenName.AudioPlayer

    var boundService: AudioPlayerService? = null

    private val translationsAdapter: BaseRecyclerViewAdapter<AudioFileModel, BaseViewHolder>
        get() = exoTranslationSelector!!.adapter.baseRecyclerViewAdapter()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            viewModel.onServiceDisconnected()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerServiceBinder
            boundService = binder.getService()

            boundService?.let {
                binding.audioPlayer.player = it.player
                viewModel.onServiceConnected(it)
            }
            setUpAudioServiceBindings()
        }
    }

    private fun setUpAudioServiceBindings() {
        boundService?.resumePlayer()
        boundService?.let { audioService ->
            audioService.audioPlayBackStatus
                .map { it is AudioPlayerService.PlayBackState.Playing }
                .bindToMain(exoPause!!.visibility())
                .disposedBy(disposeBag)

            audioService.audioPlayBackStatus
                .map { it is AudioPlayerService.PlayBackState.Paused }
                .bindToMain(exoPlay!!.visibility())
                .disposedBy(disposeBag)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exoTranslationSelector =
            binding.root.findViewById(R.id.exoTranslationSelector)
        exoPlay = binding.root.findViewById(R.id.exoPlay)
        exoPause = binding.root.findViewById(R.id.exoPause)


        exoPlay?.setOnClickListener {
            boundService?.resumePlayer()
        }

        exoPause?.setOnClickListener {
            boundService?.pausePlayer()
        }

        exoTranslationSelector!!.adapter = LanguageAdapter().toBaseAdapter()
    }

    override fun setupBindings(viewModel: AudioDetailsViewModel) {

        viewModel.title.subscribe {
            binding.expandedTitle.text = it
            binding.toolbarTitle.text = it

            val toolbarHeight = toolbar?.layoutParams?.height ?: 0
            binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener
            { _, _, scrollY, _, _ ->
                val threshold = binding.audioImage.measuredHeight + toolbarHeight / 2
                val alpha: Float = (scrollY - threshold + 40f) / 40f
                binding.toolbarTitle.alpha = MathUtils.clamp(alpha, 0f, 1f)
                binding.expandedTitle.alpha = 1 - alpha
            })
        }.disposedBy(disposeBag)

        val options = RequestOptions()
            .dontAnimate()
            .dontTransform()

        viewModel.image
            .map { it.isNotEmpty() }
            .bindToMain(binding.audioImage.visibility())
            .disposedBy(disposeBag)

        viewModel.image.subscribe {
            Glide.with(this)
                .load(it)
                .apply(options)
                .placeholder(R.color.placeholderBackground)
                .error(R.drawable.placeholder_large)
                .listenerAnimateSharedTransaction(this, binding.audioImage)
                .into(binding.audioImage)
        }.disposedBy(disposeBag)


        bindTranslationSelector(viewModel)

        viewModel.authorCulturalPlace
            .map { it.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { hasData ->
                binding.artistCulturePlaceDenim.show(show = hasData)
                binding.dividerBelowArtist.show(show = hasData)
            }
            .disposedBy(disposeBag)

        viewModel.authorCulturalPlace
            .map { it.filterHtmlEncodedText() }
            .bindToMain(binding.artistCulturePlaceDenim.text())
            .disposedBy(disposeBag)

        viewModel.transcript
            .map { it.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                binding.transcript.show(it)
            }
            .disposedBy(disposeBag)

        viewModel.transcript
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.transcript.setContentText(it.filterHtmlEncodedText())
            }.disposedBy(disposeBag)

        viewModel.tourDescription
            .map { it.isNotEmpty() }
            .bindToMain(binding.tourDescription.visibility())
            .disposedBy(disposeBag)

        viewModel.tourDescription
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.tourDescription.text = it.filterHtmlEncodedText()
            }.disposedBy(disposeBag)

        viewModel.tourIntroduction
            .map { it.isNotEmpty() }
            .bindToMain(binding.tourIntroduction.visibility())
            .disposedBy(disposeBag)

        viewModel.tourIntroduction
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.tourIntroduction.text = it.filterHtmlEncodedText()
            }.disposedBy(disposeBag)

        viewModel.credits
            .map { it.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                binding.credit.show(it)
            }
            .disposedBy(disposeBag)


        viewModel.credits
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.credit.setContentText(it.filterHtmlEncodedText())
            }.disposedBy(disposeBag)

        viewModel.relatedTours
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { tours ->
                val hasData = tours.isNotEmpty()
                binding.relatedTourTitle.show(show = hasData)
                binding.relatedToursView.show(show = hasData)
                binding.dividerBelowRelatedTours.show(show = hasData)

                if (hasData) {
                    binding.relatedToursView.removeAllViews()
                    addRelatedToursToView(tours)
                }
            }
            .disposedBy(disposeBag)
    }

    @UiThread
    fun addRelatedToursToView(tours: List<ArticTour>) {

        /**
         * Apply margin to related tour title. It is not defined in [R.style.SectionTitleWhite]
         */
        val doubleMargin = resources.getDimensionPixelSize(R.dimen.marginDouble)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            .apply {
                setMargins(doubleMargin, doubleMargin, doubleMargin, 0)
            }

        /**
         * Add related tour titles to relatedToursView [LinearLayout]
         */
        tours.forEach { tour ->
            val tourTextView = TextView(requireContext())
            tourTextView.text = tour.title
            TextViewCompat.setTextAppearance(tourTextView, R.style.RelatedTourTitleStyle)

            tourTextView.layoutParams = params
            tourTextView.setOnClickListener {
                startActivity(NavigationConstants.MAP.asDeepLinkIntent()
                    .apply {
                        putExtra(NavigationConstants.ARG_TOUR, tour)
                        putExtra(NavigationConstants.ARG_TOUR_START_STOP, tour.getIntroStop())
                        flags =
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                    }
                )
            }
            binding.relatedToursView.addView(tourTextView)
        }
    }


    private fun bindTranslationSelector(viewModel: AudioDetailsViewModel) {

        viewModel.audioTrackToUse
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { chosen ->
                exoTranslationSelector!!.setSelection(translationsAdapter.itemIndexOf(chosen))
            }
            .disposedBy(disposeBag)

        viewModel.availableTranslations
            .bindToMain(translationsAdapter.itemChanges())
            .disposedBy(disposeBag)

        viewModel.availableTranslations
            .map { it.size > 1 }
            .bindToMain(exoTranslationSelector!!.visibility(View.INVISIBLE))
            .disposedBy(disposeBag)

        exoTranslationSelector!!.itemSelections()
            .subscribe { position ->
                if (position >= 0) {
                    val translation = translationsAdapter.getItem(position)
                    viewModel.setTranslationOverride(translation)
                    boundService?.switchAudioTrack(translation)
                }
            }.disposedBy(disposeBag)

    }

    override fun onResume() {
        super.onResume()
        requireActivity().bindService(
            AudioPlayerService.getLaunchIntent(requireContext()),
            serviceConnection, BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unbindService(serviceConnection)
    }

}