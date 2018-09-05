package edu.artic.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.EditText
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicks
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.models.ArticObject
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.getAudioServiceObservable
import edu.artic.ui.findNavController
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_audio_lookup.*
import kotlinx.android.synthetic.main.fragment_audio_lookup.view.*
import java.text.BreakIterator
import kotlin.reflect.KClass

/**
 * This is the basis for a screen that finds [ArticObject]s by id.
 *
 * We want to support lookup of any String that can map to a
 * [ArticObject.objectSelectorNumber]. At current moment, all of these
 * are numeric and so the UI displayed by this class is styled after a
 * numeric keyboard.
 *
 * Business logic (like the actual lookup) is handled in [AudioLookupViewModel].
 * This class is hosted in an [AudioActivity].
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
class AudioLookupFragment : BaseViewModelFragment<AudioLookupViewModel>() {

    override val viewModelClass: KClass<AudioLookupViewModel>
        get() = AudioLookupViewModel::class
    override val title: String
        get() = ""
    override val layoutResId: Int
        get() = R.layout.fragment_audio_lookup
    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.AudioGuide

    override fun hasTransparentStatusBar(): Boolean {
        return true
    }

    private var audioService: Subject<AudioPlayerService> = BehaviorSubject.create()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This sets the hint and instructional text.
        viewModel.chosenInfo
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    view.lookup_field.hint = it.audioTitle
                    view.subheader.text = it.audioSubtitle
                    view.requestLayout()
                }.disposedBy(disposeBag)


        val numberPadAdapter = NumberPadAdapter()

        viewModel.preferredNumberPadElements
                .bindToMain(numberPadAdapter.itemChanges())
                .disposedBy(disposeBag)

        number_pad.adapter = numberPadAdapter

        numberPadAdapter.itemClicks()
                .subscribeBy { element ->
                    viewModel.adapterClicks
                            .onNext(element)
                }
                .disposedBy(disposeBag)

        registerNumPadSubscription()

        getAudioServiceObservable(fm = childFragmentManager)
                .bindTo(audioService)
                .disposedBy(disposeBag)

        viewModel.lookupResults
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { result ->
                    when (result) {
                        is LookupResult.FoundAudio -> playAndDisplay(result)
                        is LookupResult.NotFound -> shakeLookupField()
                    }
                }
                .disposedBy(disposeBag)

    }

    private fun playAndDisplay(foundAudio: LookupResult.FoundAudio) {
        audioService
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    // Send Analytics for 'playback initiated'
                    analyticsTracker.reportEvent(
                            screenCategory,
                            AnalyticsAction.playAudioAudioGuide,
                            foundAudio.hostObject.title
                    )
                    // Request the actual playback (this triggers its own analytics event)
                    it?.playPlayer(foundAudio.hostObject)
                    // Switch to the details screen
                    baseActivity.supportFragmentManager
                            .findNavController()
                            ?.navigate(R.id.peek_audio_details)
                }.disposedBy(disposeBag)
    }

    /**
     * XXX: This may somehow be split (at least partially) into the [viewModel].
     */
    private fun registerNumPadSubscription() {
        val entryField: EditText = lookup_field
        val perCharacter = BreakIterator.getCharacterInstance()

        viewModel.adapterClicks
                .subscribeBy { element ->
                    when (element) {
                        is NumberPadElement.Numeric -> {
                            if (entryField.length() < 5) {
                                entryField.append(element.value)
                            }
                        }
                        NumberPadElement.DeleteBack -> {
                            entryField.text.apply {
                                if (isNotEmpty()) {
                                    perCharacter.setText(this.toString())

                                    val end = perCharacter.last()
                                    val start = perCharacter.previous()

                                    delete(start, end)
                                }
                            }
                        }
                        NumberPadElement.GoSearch -> {
                            viewModel.lookupRequests
                                    .onNext(lookup_field.text.toString())
                        }
                    }
                }.disposedBy(disposeBag)
    }

    /**
     * Call this if the input is not usable.
     */
    private fun shakeLookupField() {
        val shaker = ObjectAnimator.ofFloat(lookup_field, View.TRANSLATION_X,
                0F, 10F, -10F)

        shaker
                // According to iOS source code, the duration should be 0.07 seconds
                .setDuration(70L)
                .apply {
                    // Back and forth four times.
                    repeatCount = 4
                    // Be explicit about preferred interpolator
                    interpolator = LinearInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animator: Animator?) {
                            // Reset to default at end of animation
                            ((animator as ObjectAnimator).target as View).translationX = 0F
                        }
                    })
                }.start()
    }
}
