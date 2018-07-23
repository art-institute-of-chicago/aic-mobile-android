package edu.artic.exhibitions

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.db.models.ArticExhibition
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_exhibition_details.*
import kotlin.reflect.KClass

class ExhibitionDetailFragment : BaseViewModelFragment<ExhibitionDetailViewModel>() {

    override val viewModelClass: KClass<ExhibitionDetailViewModel>
        get() = ExhibitionDetailViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_exhibition_details
    override val title: String
        get() = ""

    private val exhibition by lazy { arguments!!.getParcelable<ArticExhibition>(ARG_EXHIBITION) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onRegisterViewModel(viewModel: ExhibitionDetailViewModel) {
        viewModel.setExhibitionExhibition(exhibition)
    }

    override fun setupBindings(viewModel: ExhibitionDetailViewModel) {
        viewModel.title
                .bindToMain(exhibitionName.text())
                .disposedBy(disposeBag)

        viewModel.metaData
                .bindToMain(metaData.text())
                .disposedBy(disposeBag)

        viewModel.description
                .bindToMain(description.text())
                .disposedBy(disposeBag)

        viewModel.throughDate
                .bindToMain(throughDate.text())
                .disposedBy(disposeBag)

        showOnMap.clicks()
                .subscribe { viewModel.onClickShowOnMap() }
                .disposedBy(disposeBag)

        buyTickets.clicks()
                .subscribe { viewModel.onClickBuyTickets() }
                .disposedBy(disposeBag)

    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_all_exhibitions, menu)
    }

    companion object {
        private val ARG_EXHIBITION = "${ExhibitionDetailFragment::class.java.simpleName}: exhibition"

        fun newInstance(exhibition: ArticExhibition) = ExhibitionDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_EXHIBITION, exhibition)
            }
        }
    }
}