package edu.artic.accesscard

import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.REVERSE
import android.app.AlertDialog
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.content.ContextCompat
import android.view.View
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.google.zxing.BarcodeFormat
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.hintRes
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textChanges
import edu.artic.accesscard.barcode.BarcodeEncoder
import edu.artic.analytics.ScreenName
import edu.artic.base.LoadStatus
import edu.artic.base.utils.hideSoftKeyboard
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_validate_member_information.*
import kotlinx.android.synthetic.main.layout_member_access_card.*
import kotlinx.android.synthetic.main.layout_member_information_form.*
import kotlin.reflect.KClass


/**
 * This fragment is responsible for:
 *
 * 1. Displays the member information form (with memberID & ZIP code fields) for validation .
 * 2. Displays the cardHolder information with the barcode (encodes memberID to PDF417) and hides the form.
 */
class AccessMemberCardFragment : BaseViewModelFragment<AccessMemberCardViewModel>() {
    override val viewModelClass: KClass<AccessMemberCardViewModel> = AccessMemberCardViewModel::class
    override val title = R.string.accessMemberCardLabel
    override val layoutResId: Int = R.layout.fragment_validate_member_information
    override val screenName: ScreenName? = null

    private lateinit var toolbarColorAnimator: ValueAnimator

    override fun setupBindings(viewModel: AccessMemberCardViewModel) {
        super.setupBindings(viewModel)
        viewModel.isValid
                .subscribeBy {
                    signIn.isEnabled = it
                }.disposedBy(disposeBag)

        zipCode.textChanges()
                .skipInitialValue()
                .map { it.toString() }
                .bindTo(viewModel.zipCode)
                .disposedBy(disposeBag)

        memberId.textChanges()
                .skipInitialValue()
                .map { it.toString() }
                .bindTo(viewModel.memberID)
                .disposedBy(disposeBag)

        viewModel.loadStatus
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { loadStatus ->
                    when (loadStatus) {
                        is LoadStatus.Loading -> {
                            loading.visibility = View.VISIBLE
                            mask.visibility = View.VISIBLE
                        }
                        is LoadStatus.Error -> {
                            loading.visibility = View.GONE
                            mask.visibility = View.GONE
                            /**
                             * Designs does not exist for error dialogs yet.
                             * Temporarily using [AlertDialog].
                             *
                             * TODO:: Upgrade to DialogFragment once design is finalized.
                             */
                            AlertDialog.Builder(requireContext(), R.style.ErrorDialog)
                                    .setTitle(getString(R.string.errorDialogTitle))
                                    .setMessage(loadStatus.error.message)
                                    .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                                        dialog.dismiss()
                                    }.show()
                        }
                        is LoadStatus.None -> {
                            loading.visibility = View.GONE
                            mask.visibility = View.GONE
                        }
                    }
                }.disposedBy(disposeBag)

        viewModel.displayMode
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    when (it) {
                        AccessMemberCardViewModel.DisplayMode.DisplayForm -> {
                            memberInfoFormLayout.visibility = View.VISIBLE
                            memberAccessCardLayout.visibility = View.GONE
                            requireActivity().setTitle(R.string.accessMemberCardLabel)
                        }
                        is AccessMemberCardViewModel.DisplayMode.DisplayAccessCard -> {
                            memberInfoFormLayout.visibility = View.GONE
                            memberAccessCardLayout.visibility = View.VISIBLE
                            updateBarCode(it.memberID)
                            requireActivity().setTitle(R.string.memberCard)
                        }
                        is AccessMemberCardViewModel.DisplayMode.UpdateForm -> {
                            memberInfoFormLayout.visibility = View.VISIBLE
                            memberAccessCardLayout.visibility = View.GONE
                            requireActivity().setTitle(R.string.accessMemberCardLabel)
                            zipCode.setText(it.zipCode)
                            memberId.setText(it.memberID)
                        }
                    }
                }.disposedBy(disposeBag)

        viewModel.selectedCardHolder
                .observeOn(AndroidSchedulers.mainThread())
                .bindTo(cardHolder.text())
                .disposedBy(disposeBag)

        viewModel.membership
                .observeOn(AndroidSchedulers.mainThread())
                .bindTo(membershipType.text())
                .disposedBy(disposeBag)

        viewModel.expiration
                .map { resources.getString(R.string.expires, it) }
                .bindToMain(expiration.text())
                .disposedBy(disposeBag)

        switchCardHolder.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onSwitchCardholderClicked()
                }.disposedBy(disposeBag)

        changeInformation.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onUpdateInformationClicked()
                }.disposedBy(disposeBag)

        viewModel.zipCodeHint
                .bindToMain(zipCode.hintRes())
                .disposedBy(disposeBag)

        viewModel.memberIdHint
                .bindToMain(memberId.hintRes())
                .disposedBy(disposeBag)

        viewModel.members
                .map { it.size > 1 }
                .bindToMain(switchCardHolder.visibility())
                .disposedBy(disposeBag)
    }

    /**
     * Generate the barcode using memberID.
     */
    private fun updateBarCode(memberID: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()

            val barCodeHeight = resources.getDimensionPixelSize(R.dimen.barcodeHeight)
            val barCodeWidth = resources.displayMetrics.widthPixels

            val bitmap = barcodeEncoder.encodeBitmap(memberID, BarcodeFormat.PDF_417, barCodeWidth, barCodeHeight)
            barcode.setImageBitmap(bitmap)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        signIn.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onSignInClick()
                    requireActivity().hideSoftKeyboard()
                }.disposedBy(disposeBag)


        animateToolbarColor()
    }

    /**
     * Animates the color of toolbar and StatusBar.
     */
    @UiThread
    private fun animateToolbarColor() {
        val fromColor = ContextCompat.getColor(requireContext(), R.color.brownishOrange)
        val toColor = ContextCompat.getColor(requireContext(), R.color.infoScreenRed)

        toolbarColorAnimator = ValueAnimator.ofArgb(fromColor, toColor)
        toolbarColorAnimator.addUpdateListener { valueAnimator ->
            requireActivity().window.statusBarColor = valueAnimator.animatedValue as Int
            toolbar?.setBackgroundColor(valueAnimator.animatedValue as Int)
        }
        toolbarColorAnimator.duration = 1000
        toolbarColorAnimator.repeatCount = INFINITE
        toolbarColorAnimator.repeatMode = REVERSE
        toolbarColorAnimator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toolbarColorAnimator.cancel()
    }
}
