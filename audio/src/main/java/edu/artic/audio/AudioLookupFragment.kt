package edu.artic.audio

import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.BaseRecyclerViewAdapter.OnItemClickListener
import edu.artic.adapter.BaseViewHolder
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_audio_lookup.*
import java.text.BreakIterator
import kotlin.reflect.KClass

/**
 * This is the basis for a screen that finds [ArticAudioFile]s for by id.
 *
 * We want to support lookup of any String that can map to a [ArticAudioFile.nid].
 * At current moment, all of these are numeric and so the UI displayed by this
 * class is styled after a numeric keyboard.
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
    override val screenCategory: ScreenCategoryName?
        get() = ScreenCategoryName.AudioGuide


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val numberPadAdapter = NumberPadAdapter()

        viewModel.preferredNumberPadElements
                .bindToMain(numberPadAdapter.itemChanges())
                .disposedBy(disposeBag)

        number_pad.adapter = numberPadAdapter
        numberPadAdapter.onItemClickListener = NumPadClickListener(lookup_field)
    }


    /**
     * This on-click-listener adds and removes numbers to the [entryField] it wraps.
     */
    class NumPadClickListener(private val entryField: EditText) : OnItemClickListener<NumberPadElement> {

        private val perCharacter = BreakIterator.getCharacterInstance()

        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun onItemClick(position: Int, element: NumberPadElement, holder: BaseViewHolder) {
            when (element) {
                is NumberPadElement.Numeric -> {
                    entryField.append(element.value)
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
                NumberPadElement.GoSearch -> TODO()
            }
        }
    }
}