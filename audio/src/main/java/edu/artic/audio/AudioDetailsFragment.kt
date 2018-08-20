package edu.artic.audio


import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.listenerAnimateSharedTransaction
import edu.artic.base.utils.updateDetailTitle
import edu.artic.db.models.AudioTranslation
import edu.artic.localization.BaseTranslation
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.refreshPlayBackState
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.fragment_audio_details.*
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
class AudioDetailsFragment : BaseViewModelFragment<AudioDetailsViewModel>() {

    override val viewModelClass: KClass<AudioDetailsViewModel>
        get() = AudioDetailsViewModel::class

    override val title: String
        get() = ""

    override val layoutResId: Int
        get() = R.layout.fragment_audio_details

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.AudioPlayer

    override fun hasTransparentStatusBar(): Boolean = true
    var boundService: AudioPlayerService? = null
    private var audioIntent: Intent? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            viewModel.audioObject = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlayerService.AudioPlayerServiceBinder
            boundService = binder.getService()
            viewModel.audioObject = boundService?.articObject

            boundService?.let {
                audioPlayer.player = it.player
                it.player.refreshPlayBackState()
            }

        }
    }

    override fun setupBindings(viewModel: AudioDetailsViewModel) {

        viewModel.title.subscribe {
            expandedTitle.text = it
            toolbarTitle.text = it
        }.disposedBy(disposeBag)

        val options = RequestOptions()
                .dontAnimate()
                .dontTransform()

        viewModel.image
                .map { it.isNotEmpty() }
                .bindToMain(audioImage.visibility())
                .disposedBy(disposeBag)

        viewModel.image.subscribe {
            Glide.with(this)
                    .load(it)
                    .apply(options)
                    .listenerAnimateSharedTransaction(this, audioImage)
                    .into(audioImage)
        }.disposedBy(disposeBag)



        bindTranslationSelector(viewModel)



        viewModel.authorCulturalPlace
                .map { it.isNotEmpty() }
                .bindToMain(artistCulturePlaceDenim.visibility())
                .disposedBy(disposeBag)

        viewModel.authorCulturalPlace
                .bindToMain(artistCulturePlaceDenim.text())
                .disposedBy(disposeBag)

        viewModel.transcript
                .map { it.isNotEmpty() }
                .bindToMain(transcript.visibility())
                .disposedBy(disposeBag)

        viewModel.transcript
                .subscribe {
                    transcript.setContentText(it)
                }.disposedBy(disposeBag)

        viewModel.credits
                .map { it.isNotEmpty() }
                .bindToMain(credit.visibility())
                .disposedBy(disposeBag)


        viewModel.credits
                .subscribe {
                    credit.setContentText(it)
                }.disposedBy(disposeBag)
    }


    private fun bindTranslationSelector(viewModel: AudioDetailsViewModel) {
        // TODO: Consider replacing 'Spinner' with a more capable class; possibly one from a 3-party library
        val selectorView: Spinner = exo_translation_selector
        viewModel.availableTranslations
                .map {
                    LanguageAdapter(selectorView.context, it)
                }.bindToMain(Consumer<LanguageAdapter<AudioTranslation>> { la: LanguageAdapter<AudioTranslation> ->
                    selectorView.apply {
                        this.adapter = la
                        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: AdapterView<*>) {
                                // Nothing to be done. Perhaps we could reset our language selection?
                            }

                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                if (position >= 0) {
                                    val translation = la.getItem(position)
                                    viewModel.setTranslationOverride(translation)
                                    boundService?.switchAudioTrack(translation)
                                }
                            }
                        }
                        this.setSelection(la.getPosition(viewModel.chosenTranslation.value))
                    }
                })
                .disposedBy(disposeBag)
    }


    override fun onResume() {
        super.onResume()
        audioIntent = AudioPlayerService.getLaunchIntent(requireContext())
        requireActivity().bindService(audioIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unbindService(serviceConnection)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }
    }
}

/**
 * List adapter for the language-selection dropdown.
 *
 * This is also responsible for creating the view seen at the top
 * of the list (i.e. the 'currently selected' language).
 */
class LanguageAdapter<T : BaseTranslation>(context: Context, translations: List<T>) : ArrayAdapter<T>(
        context,
        R.layout.view_language_box,
        translations
) {
    // This is a view for use in the dropdown...
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return inflateAndBind(position, convertView, parent)
    }

    // ..and this is the one used to preview the current selection
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): TextView {
        val derived = inflateAndBind(position, convertView, parent)

        derived.setBackgroundResource(R.drawable.translation_selection)
        derived.setTextColor(Color.WHITE)

        return derived
    }

    /**
     * Inflate and return a copy of `view_language_box.xml`.
     *
     * The inflated [TextView] will display the text of a [BaseTranslation] at the
     * [provided index][position] within this adapter. Please refer to
     * [BaseTranslation.userFriendlyLanguage] for the expected textual output.
     *
     * @see ArrayAdapter.getView
     */
    private fun inflateAndBind(position: Int, convertView: View?, parent: ViewGroup): TextView {
        val derived = if (convertView == null) {
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_language_box, parent, false)
        } else {
            convertView
        } as TextView

        val item = getItem(position)

        derived.text = item.userFriendlyLanguage(derived.context)

        return derived
    }
}
